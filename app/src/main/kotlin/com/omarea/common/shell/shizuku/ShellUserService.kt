package com.omarea.common.shell.shizuku

import java.io.BufferedReader
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.locks.ReentrantLock

/**
 * User Service của Shizuku: chạy trong 1 tiến trình RIÊNG, mang danh tính (UID) root hoặc shell
 * tùy theo Shizuku đang chạy bằng root hay adb - xem Shizuku.getUid(). Vì tiến trình này đã sẵn
 * quyền cao, chỉ cần "sh" bình thường, không cần "su".
 *
 * Bắt buộc có constructor không tham số (tương thích Shizuku bản cũ hơn v13, bản v13+ ưu tiên
 * constructor có Context nhưng sẽ fallback về constructor này nếu không thấy).
 *
 * Nhận lệnh qua execCommand() (gọi từ app qua Binder), thực thi trên 1 tiến trình "sh" duy nhất
 * được giữ sống xuyên suốt (giống cơ chế marker của KeepShell ở phía app, nhưng chạy ngay bên
 * trong tiến trình đặc quyền này), trả về toàn bộ output. destroy() được Shizuku gọi khi app
 * unbind - dọn dẹp tiến trình con rồi thoát hẳn tiến trình service này.
 */
class ShellUserService : IShellUserService.Stub() {
    private var process: Process? = null
    private var out: OutputStream? = null
    private var reader: BufferedReader? = null
    private val lock = ReentrantLock()

    private val startTag = "|SH>>|"
    private val endTag = "|<<SH|"
    private val startTagBytes = "\necho '$startTag'\n".toByteArray(Charset.defaultCharset())
    private val endTagBytes = "\necho '$endTag'\n".toByteArray(Charset.defaultCharset())

    private fun ensureProcess() {
        if (process != null) return
        process = ProcessBuilder("sh").redirectErrorStream(true).start()
        out = process!!.outputStream
        reader = process!!.inputStream.bufferedReader()
    }

    override fun execCommand(cmd: String?): String {
        if (cmd == null) return ""
        lock.lock()
        try {
            ensureProcess()
            val output = StringBuilder()
            out?.let {
                it.write(startTagBytes)
                it.write(cmd.toByteArray(Charset.defaultCharset()))
                it.write(endTagBytes)
                it.flush()
            }

            var unstart = true
            while (reader != null) {
                val line = reader!!.readLine() ?: break
                if (line.contains(endTag)) {
                    output.append(line.substringBefore(endTag))
                    break
                } else if (line.contains(startTag)) {
                    output.clear()
                    output.append(line.substring(line.indexOf(startTag) + startTag.length))
                    unstart = false
                } else if (!unstart) {
                    output.append(line)
                    output.append("\n")
                }
            }
            return output.toString().trim()
        } catch (e: Exception) {
            try {
                process?.destroy()
            } catch (_: Exception) {
            }
            process = null
            out = null
            reader = null
            return "error"
        } finally {
            lock.unlock()
        }
    }

    override fun destroy() {
        lock.lock()
        try {
            out?.close()
            reader?.close()
            process?.destroy()
        } catch (_: Exception) {
        } finally {
            lock.unlock()
        }
        System.exit(0)
    }
}
