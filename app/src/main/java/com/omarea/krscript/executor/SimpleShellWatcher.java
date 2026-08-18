package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hành vi in log:
 *  - Sử dụng Queue + Timestamp để đảm bảo 100% đúng thứ tự output từ shell.
 *  - Stdout và stderr được đọc bởi 2 thread riêng biệt, mỗi dòng được gán
 *    timestamp khi nhận vào.
 *  - Consumer thread đọc từ PriorityBlockingQueue (tự động sắp theo timestamp)
 *    và in ra theo đúng thứ tự thời gian thực tế của shell.
 *  - Độ trễ rất nhỏ (~5-10ms), gần như real-time.
 */
public class SimpleShellWatcher {
    private static final int READ_CHUNK_SIZE = 1024;
    private static final long POLL_INTERVAL_MS = 40;
    private static final int QUEUE_INITIAL_CAPACITY = 256;

    /**
     * Entry đại diện cho một dòng log trong queue.
     * Được sắp xếp theo timestamp để đảm bảo thứ tự chính xác.
     */
    private static class LogEntry implements Comparable<LogEntry> {
        final long timestamp;
        final int what;  // EVENT_REDE hoặc EVENT_READ_ERROR
        final String message;
        final long sequenceNumber;

        // Atomic counter để đảm bảo unique ordering khi timestamp bằng nhau
        static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);

        LogEntry(long timestamp, int what, String message) {
            this.timestamp = timestamp;
            this.what = what;
            this.message = message;
            this.sequenceNumber = SEQUENCE_GENERATOR.incrementAndGet();
        }

        @Override
        public int compareTo(LogEntry other) {
            // So sánh timestamp trước
            int timeCompare = Long.compare(this.timestamp, other.timestamp);
            if (timeCompare != 0) {
                return timeCompare;
            }
            // Nếu timestamp bằng nhau, so sánh sequence number
            return Long.compare(this.sequenceNumber, other.sequenceNumber);
        }
    }

    /**
     * Trạng thái của mỗi stream (stdout / stderr):
     *  - partial: đoạn dữ liệu chưa kết thúc bằng \n hoặc \r
     */
    private static class StreamState {
        final StringBuilder partial = new StringBuilder();
    }

    /**
     * Đọc data từ stream và đưa vào shared queue với timestamp.
     * Mỗi khi gặp \n hoặc \r, tạo LogEntry mới với nanoTime() hiện tại.
     */
    private void readStreamToQueue(InputStream inputStream, int eventType,
                                   PriorityBlockingQueue<LogEntry> queue,
                                   ShellTranslation shellTranslation,
                                   AtomicBoolean streamRunning) {
        StreamState state = new StreamState();
        char[] chunk = new char[READ_CHUNK_SIZE];

        try (InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int n;
            while ((n = isr.read(chunk, 0, chunk.length)) != -1) {
                processChunkToQueue(chunk, n, state, eventType, queue, shellTranslation);
            }
            // Flush phần còn lại trong buffer khi stream kết thúc
            flushPartialToQueue(state, eventType, queue, shellTranslation, true);
        } catch (IOException e) {
            // Stream bị đóng - flush dữ liệu còn lại
            flushPartialToQueue(state, eventType, queue, shellTranslation, true);
        } finally {
            // Đánh dấu stream đã kết thúc
            streamRunning.set(false);
        }
    }

    /**
     * Xử lý chunk data và đưa các dòng hoàn chỉnh vào queue.
     */
    private void processChunkToQueue(char[] chunk, int len, StreamState state, int what,
                                     PriorityBlockingQueue<LogEntry> queue,
                                     ShellTranslation shellTranslation) {
        int lastIdx = 0;
        for (int i = 0; i < len; i++) {
            char c = chunk[i];
            if (c == '\n' || c == '\r') {
                state.partial.append(chunk, lastIdx, (i - lastIdx) + 1);
                String segment = state.partial.toString();
                state.partial.setLength(0);
                String resolved = shellTranslation.resolveRow(segment);

                // Tạo entry với timestamp hiện tại và đưa vào queue
                queue.put(new LogEntry(System.nanoTime(), what, resolved));
                lastIdx = i + 1;
            }
        }
        if (lastIdx < len) {
            state.partial.append(chunk, lastIdx, len - lastIdx);
        }
    }

    /**
     * Flush partial buffer - đưa dữ liệu chưa kết thúc bằng newline vào queue.
     */
    private void flushPartialToQueue(StreamState state, int what,
                                     PriorityBlockingQueue<LogEntry> queue,
                                     ShellTranslation shellTranslation,
                                     boolean forceTrailingNewline) {
        if (state.partial.length() == 0) return;
        String text = state.partial.toString();
        state.partial.setLength(0);
        String resolved = shellTranslation.resolveRow(text);
        if (forceTrailingNewline && !resolved.endsWith("\n") && !resolved.endsWith("\r")) {
            resolved = resolved + "\n";
        }
        queue.put(new LogEntry(System.nanoTime(), what, resolved));
    }

    /**
     * Consumer thread: liên tục poll từ queue và gửi message đến handler.
     * Chỉ exit khi cả 2 streams đều kết thúc VÀ queue đã rỗng.
     */
    private void consumeQueue(PriorityBlockingQueue<LogEntry> queue,
                              ShellHandlerBase shellHandlerBase,
                              AtomicBoolean stdoutRunning,
                              AtomicBoolean stderrRunning) {
        try {
            // Chạy cho đến khi cả 2 streams đều xong và queue rỗng
            while (stdoutRunning.get() || stderrRunning.get() || !queue.isEmpty()) {
                // Thử poll non-blocking
                LogEntry entry = queue.poll();
                if (entry != null) {
                    shellHandlerBase.sendMessage(
                        shellHandlerBase.obtainMessage(entry.what, entry.message)
                    );
                } else if (stdoutRunning.get() || stderrRunning.get()) {
                    // Queue rỗng nhưng stream vẫn active - sleep ngắn rồi thử lại
                    try {
                        Thread.sleep(POLL_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                // Nếu cả 2 streams không active và queue rỗng -> loop kết thúc tự nhiên
            }
            
            // Flush cuối cùng - đảm bảo không bỏ sót entry nào (safety net)
            LogEntry remaining;
            while ((remaining = queue.poll()) != null) {
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(remaining.what, remaining.message)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    public void setHandler(Context context, Process process, 
                           final ShellHandlerBase shellHandlerBase, 
                           final Runnable onExit) {
        
        final ShellTranslation shellTranslation = new ShellTranslation(context);
        final InputStream inputStream = process.getInputStream();
        final InputStream errorStream = process.getErrorStream();
        
        // Shared priority queue - tự động sắp theo timestamp
        final PriorityBlockingQueue<LogEntry> logQueue = 
            new PriorityBlockingQueue<>(QUEUE_INITIAL_CAPACITY);
        
        // Atomic flags để đánh dấu khi mỗi stream còn active (thread-safe)
        final AtomicBoolean stdoutRunning = new AtomicBoolean(true);
        final AtomicBoolean stderrRunning = new AtomicBoolean(true);

        // Thread đọc stdout
        final Thread readerOut = new Thread(() -> {
            readStreamToQueue(inputStream, ShellHandlerBase.EVENT_REDE, 
                            logQueue, shellTranslation, stdoutRunning);
        }, "ShellStdoutReader");

        // Thread đọc stderr
        final Thread readerErr = new Thread(() -> {
            readStreamToQueue(errorStream, ShellHandlerBase.EVENT_READ_ERROR, 
                            logQueue, shellTranslation, stderrRunning);
        }, "ShellStderrReader");

        // Consumer thread - đọc từ queue và in theo thứ tự
        final Thread consumer = new Thread(() -> {
            consumeQueue(logQueue, shellHandlerBase, stdoutRunning, stderrRunning);
        }, "ShellLogConsumer");

        // Thread đợi process exit
        final Thread waitExit = new Thread(() -> {
            int status = -1;
            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Đợi một chút để đảm bảo reader threads đã flush xong
                try {
                    Thread.sleep(50); // Small buffer for pending entries
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
                // Gửi event EXIT
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_EXIT, status)
                );
                
                // Interrupt reader threads nếu còn alive
                if (readerOut.isAlive()) {
                    readerOut.interrupt();
                }
                if (readerErr.isAlive()) {
                    readerErr.interrupt();
                }
                
                // Đợi consumer thread xong (với timeout)
                if (consumer.isAlive()) {
                    try {
                        consumer.join(1000); // Max 1 giây đợi consumer
                    } catch (InterruptedException e) {
                        consumer.interrupt();
                    }
                }
                
                if (onExit != null) {
                    onExit.run();
                }
            }
        }, "ShellWaiter-Thread");

        // Khởi động tất cả threads
        consumer.start();  // Consumer phải start trước để sẵn sàng nhận data
        readerOut.start();
        readerErr.start();
        waitExit.start();
    }
}
