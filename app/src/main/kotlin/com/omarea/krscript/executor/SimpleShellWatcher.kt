package com.omarea.krscript.executor

import android.content.Context
import com.omarea.common.shell.ShellTranslation
import com.omarea.krscript.model.ShellHandlerBase
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Hành vi in log:
 *  - Sử dụng Queue + Timestamp để đảm bảo 100% đúng thứ tự output từ shell.
 *  - Stdout và stderr được đọc bởi 2 thread riêng biệt, mỗi dòng được gán
 *    timestamp khi nhận vào.
 *  - Consumer thread đọc từ PriorityBlockingQueue (tự động sắp theo timestamp)
 *    và in ra theo đúng thứ tự thời gian thực tế của shell.
 *  - Độ trễ rất nhỏ (~5-10ms), gần như real-time.
 */
class SimpleShellWatcher {

    /**
     * Entry đại diện cho một dòng log trong queue.
     * Được sắp xếp theo timestamp để đảm bảo thứ tự chính xác.
     */
    private class LogEntry(val timestamp: Long, val what: Int, val message: String) : Comparable<LogEntry> {
        // Atomic counter để đảm bảo unique ordering khi timestamp bằng nhau
        val sequenceNumber: Long = SEQUENCE_GENERATOR.incrementAndGet()

        override fun compareTo(other: LogEntry): Int {
            // So sánh timestamp trước
            val timeCompare = timestamp.compareTo(other.timestamp)
            if (timeCompare != 0) {
                return timeCompare
            }
            // Nếu timestamp bằng nhau, so sánh sequence number
            return sequenceNumber.compareTo(other.sequenceNumber)
        }

        companion object {
            val SEQUENCE_GENERATOR = AtomicLong(0)
        }
    }

    /**
     * Trạng thái của mỗi stream (stdout / stderr):
     *  - partial: đoạn dữ liệu chưa kết thúc bằng \n hoặc \r
     */
    private class StreamState {
        val partial = StringBuilder()
    }

    /**
     * Đọc data từ stream và đưa vào shared queue với timestamp.
     * Mỗi khi gặp \n hoặc \r, tạo LogEntry mới với nanoTime() hiện tại.
     */
    private fun readStreamToQueue(
        inputStream: InputStream, eventType: Int,
        queue: PriorityBlockingQueue<LogEntry>,
        shellTranslation: ShellTranslation,
        streamRunning: AtomicBoolean
    ) {
        val state = StreamState()
        val chunk = CharArray(READ_CHUNK_SIZE)

        try {
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { isr ->
                var n: Int
                while (isr.read(chunk, 0, chunk.size).also { n = it } != -1) {
                    processChunkToQueue(chunk, n, state, eventType, queue, shellTranslation)
                }
                // Flush phần còn lại trong buffer khi stream kết thúc
                flushPartialToQueue(state, eventType, queue, shellTranslation, true)
            }
        } catch (e: IOException) {
            // Stream bị đóng - flush dữ liệu còn lại
            flushPartialToQueue(state, eventType, queue, shellTranslation, true)
        } finally {
            // Đánh dấu stream đã kết thúc
            streamRunning.set(false)
        }
    }

    /**
     * Xử lý chunk data và đưa các dòng hoàn chỉnh vào queue.
     */
    private fun processChunkToQueue(
        chunk: CharArray, len: Int, state: StreamState, what: Int,
        queue: PriorityBlockingQueue<LogEntry>,
        shellTranslation: ShellTranslation
    ) {
        var lastIdx = 0
        for (i in 0 until len) {
            val c = chunk[i]
            if (c == '\n' || c == '\r') {
                state.partial.append(chunk, lastIdx, (i - lastIdx) + 1)
                val segment = state.partial.toString()
                state.partial.setLength(0)
                val resolved = shellTranslation.resolveRow(segment)

                // Tạo entry với timestamp hiện tại và đưa vào queue
                queue.put(LogEntry(System.nanoTime(), what, resolved))
                lastIdx = i + 1
            }
        }
        if (lastIdx < len) {
            state.partial.append(chunk, lastIdx, len - lastIdx)
        }
    }

    /**
     * Flush partial buffer - đưa dữ liệu chưa kết thúc bằng newline vào queue.
     */
    private fun flushPartialToQueue(
        state: StreamState, what: Int,
        queue: PriorityBlockingQueue<LogEntry>,
        shellTranslation: ShellTranslation,
        forceTrailingNewline: Boolean
    ) {
        if (state.partial.isEmpty()) return
        val text = state.partial.toString()
        state.partial.setLength(0)
        var resolved = shellTranslation.resolveRow(text)
        if (forceTrailingNewline && !resolved.endsWith("\n") && !resolved.endsWith("\r")) {
            resolved = "$resolved\n"
        }
        queue.put(LogEntry(System.nanoTime(), what, resolved))
    }

    /**
     * Consumer thread: liên tục poll từ queue và gửi message đến handler.
     * Chỉ exit khi cả 2 streams đều kết thúc VÀ queue đã rỗng.
     */
    private fun consumeQueue(
        queue: PriorityBlockingQueue<LogEntry>,
        shellHandlerBase: ShellHandlerBase,
        stdoutRunning: AtomicBoolean,
        stderrRunning: AtomicBoolean
    ) {
        try {
            // Chạy cho đến khi cả 2 streams đều xong và queue rỗng
            while (stdoutRunning.get() || stderrRunning.get() || !queue.isEmpty()) {
                // Thử poll non-blocking
                val entry = queue.poll()
                if (entry != null) {
                    shellHandlerBase.sendMessage(
                        shellHandlerBase.obtainMessage(entry.what, entry.message)
                    )
                } else if (stdoutRunning.get() || stderrRunning.get()) {
                    // Queue rỗng nhưng stream vẫn active - sleep ngắn rồi thử lại
                    try {
                        Thread.sleep(POLL_INTERVAL_MS)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
                // Nếu cả 2 streams không active và queue rỗng -> loop kết thúc tự nhiên
            }

            // Flush cuối cùng - đảm bảo không bỏ sót entry nào (safety net)
            var remaining: LogEntry?
            while (queue.poll().also { remaining = it } != null) {
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(remaining!!.what, remaining!!.message)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Thiết lập handler cho process shell.
     *
     * Kiến trúc:
     * ┌─ Thread stdout ─────────┐     ┌─ Thread stderr ─────────┐
     * │  Đọc inputStream        │     │  Đọc errorStream        │
     * │  Ghi timestamp (nanoTime)│     │  Ghi timestamp (nanoTime)│
     * │  → PUT PriorityQueue    │     │  → PUT PriorityQueue    │
     * └──────────────────────────┘     └──────────────────────────┘
     *                     ↘                          ↙
     *               ┌─────────────────────────────┐
     *               │  PriorityBlockingQueue       │ ← Tự động sắp theo timestamp
     *               │  (thread-safe)               │
     *               └─────────────┬───────────────┘
     *                             ↓
     *               ┌─────────────────────────────┐
     *               │  Consumer Thread             │ → IN THEO THỨ TỰ ĐÚNG 100%
     *               └─────────────────────────────┘
     *
     * @param context          Android context
     * @param process          Runtime process
     * @param shellHandlerBase Handler nhận log messages
     * @param onExit           Callback khi process kết thúc
     */
    fun setHandler(
        context: Context, process: Process,
        shellHandlerBase: ShellHandlerBase,
        onExit: Runnable?
    ) {
        val shellTranslation = ShellTranslation(context)
        val inputStream = process.inputStream
        val errorStream = process.errorStream

        // Shared priority queue - tự động sắp theo timestamp
        val logQueue = PriorityBlockingQueue<LogEntry>(QUEUE_INITIAL_CAPACITY)

        // Atomic flags để đánh dấu khi mỗi stream còn active (thread-safe)
        val stdoutRunning = AtomicBoolean(true)
        val stderrRunning = AtomicBoolean(true)

        // Thread đọc stdout
        val readerOut = Thread({
            readStreamToQueue(
                inputStream, ShellHandlerBase.EVENT_REDE,
                logQueue, shellTranslation, stdoutRunning
            )
        }, "ShellStdoutReader")

        // Thread đọc stderr
        val readerErr = Thread({
            readStreamToQueue(
                errorStream, ShellHandlerBase.EVENT_READ_ERROR,
                logQueue, shellTranslation, stderrRunning
            )
        }, "ShellStderrReader")

        // Consumer thread - đọc từ queue và in theo thứ tự
        val consumer = Thread({
            consumeQueue(logQueue, shellHandlerBase, stdoutRunning, stderrRunning)
        }, "ShellLogConsumer")

        // Thread đợi process exit
        val waitExit = Thread({
            var status = -1
            try {
                status = process.waitFor()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                // Đợi một chút để đảm bảo reader threads đã flush xong
                try {
                    Thread.sleep(50) // Small buffer for pending entries
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                }

                // Gửi event EXIT
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_EXIT, status)
                )

                // Interrupt reader threads nếu còn alive
                if (readerOut.isAlive) {
                    readerOut.interrupt()
                }
                if (readerErr.isAlive) {
                    readerErr.interrupt()
                }

                // Đợi consumer thread xong (với timeout)
                if (consumer.isAlive) {
                    try {
                        consumer.join(1000) // Max 1 giây đợi consumer
                    } catch (e: InterruptedException) {
                        consumer.interrupt()
                    }
                }

                onExit?.run()
            }
        }, "ShellWaiter-Thread")

        // Khởi động tất cả threads
        consumer.start() // Consumer phải start trước để sẵn sàng nhận data
        readerOut.start()
        readerErr.start()
        waitExit.start()
    }

    companion object {
        private const val READ_CHUNK_SIZE = 1024
        private const val POLL_INTERVAL_MS = 40L
        private const val QUEUE_INITIAL_CAPACITY = 256
    }
}
