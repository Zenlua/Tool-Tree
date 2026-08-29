package com.omarea.krscript.downloader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import androidx.lifecycle.LifecycleCoroutineScope
import com.tool.tree.R
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.DownloadNode
import com.omarea.krscript.model.ShellHandlerBase
import com.omarea.krscript.ui.ListItemDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// Xử lý cho loại mục [[download]]: tải file qua HTTP(S) - tiến trình cập nhật NGAY trên
// ListItemDownload (không dialog riêng) - lưu vào cache riêng của app với tên NGẪU NHIÊN, sau
// đó chạy script (RunnableNode.setState) với biến môi trường $state = đường dẫn file vừa tải.
// Trong lúc đang TẢI, bấm lại vào item sẽ HUỶ (xem ListItemDownload.cancelIfDownloading()) -
// nhưng KHÔNG thể huỷ nữa một khi đã chuyển sang chạy script (script đã bắt đầu chạy dưới shell,
// không có cách nào an toàn để dừng giữa chừng).
object DownloadTaskHelper {
    // Khoảng thời gian (ms) giữ nguyên nhãn trạng thái cuối (đang chạy/thành công/lỗi) trên
    // màn hình trước khi báo hoàn tất cho bên gọi (mở khoá item lại, xử lý reload-page...) -
    // để người dùng thực sự kịp nhìn thấy kết quả thay vì nó biến mất ngay lập tức.
    private const val STATUS_HOLD_MS = 900L

    // onFinished: gọi ĐÚNG 1 LẦN khi toàn bộ phiên (tải + chạy script, dù thành công hay lỗi)
    // kết thúc - bên gọi dùng để gọi tiếp krScriptActionHandler.onActionCompleted(item) (xử lý
    // reload-page/auto-finish/auto-kill/auto-restart) và onExit gốc của PageLayoutRender. KHÔNG
    // gọi onFinished khi người dùng chủ động huỷ tải giữa chừng (xem bên dưới) - vì chưa có gì
    // thay đổi thực sự để mà reload/auto-finish.
    fun start(
        context: Context,
        scope: LifecycleCoroutineScope,
        item: DownloadNode,
        view: ListItemDownload,
        onFinished: () -> Unit
    ) {
        if (view.isBusy) return

        // Cờ + tham chiếu connection dùng chung giữa lambda huỷ (chạy trên main thread, do
        // người dùng bấm) và coroutine IO đang tải (xem downloadToFile) - connection.disconnect()
        // là cách duy nhất "đánh thức" được input.read() đang block giữa chừng.
        var cancelled = false
        var liveConnection: HttpURLConnection? = null

        view.markBusy {
            cancelled = true
            try {
                liveConnection?.disconnect()
            } catch (_: Exception) {
            }
        }

        scope.launch(Dispatchers.IO) {
            val destFile = try {
                File(context.cacheDir, "kr_download_" + UUID.randomUUID().toString().replace("-", "") + guessSuffix(item.url))
            } catch (ex: Exception) {
                null
            }

            val error = if (destFile == null) {
                "invalid destination"
            } else {
                downloadToFile(
                    url = item.url,
                    destFile = destFile,
                    onConnectionOpened = { conn -> liveConnection = conn },
                    onProgress = { downloaded, total ->
                        // Vẫn có thể có vài tick tiến trình tới trễ ngay sau lúc cancelled=true
                        // (đã gọi disconnect() nhưng vòng đọc chưa kịp nhận IOException) - bỏ
                        // qua để không "hồi sinh" UI đang trong lúc chuẩn bị đóng lại.
                        postMain { if (!cancelled) view.updateDownloadProgress(downloaded, total) }
                    }
                )
            }

            postMain {
                view.clearCancelAction()

                if (cancelled) {
                    destFile?.delete()
                    view.restoreDesc()
                    view.finishBusy()
                    return@postMain
                }

                if (error != null || destFile == null) {
                    view.showStatusLabel(context.getString(R.string.kr_download_error))
                    finishAfterDelay(view, onFinished)
                } else {
                    runPostScript(context, item, view, destFile, onFinished)
                }
            }
        }
    }

    private fun postMain(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

    private fun finishAfterDelay(view: ListItemDownload, onFinished: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            view.finishBusy()
            onFinished()
        }, STATUS_HOLD_MS)
    }

    private fun guessSuffix(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        val name = clean.substringAfterLast('/')
        val dot = name.lastIndexOf('.')
        return if (dot in 1 until name.length - 1) name.substring(dot) else ""
    }

    // Trả về null nếu tải thành công, hoặc thông báo lỗi (không ném exception ra ngoài). Khi bị
    // huỷ giữa chừng (connection.disconnect() gọi từ luồng khác), input.read() sẽ ném IOException
    // như 1 lỗi mạng bình thường - bên gọi (start()) tự phân biệt qua cờ [cancelled] chứ không
    // dựa vào nội dung lỗi trả về ở đây.
    private fun downloadToFile(
        url: String,
        destFile: File,
        onConnectionOpened: (HttpURLConnection) -> Unit,
        onProgress: (Long, Long) -> Unit
    ): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
            }
            connection.connect()
            onConnectionOpened(connection)
            if (connection.responseCode !in 200..299) {
                return "HTTP ${connection.responseCode}"
            }
            val total = connection.contentLengthLong
            var downloaded = 0L
            var lastReported = 0L
            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        // Chỉ báo tiến trình mỗi >=32KB (hoặc lần đầu) để tránh spam main thread
                        if (downloaded - lastReported >= 32 * 1024 || lastReported == 0L) {
                            lastReported = downloaded
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
            onProgress(downloaded, total)
            null
        } catch (ex: Exception) {
            "" + ex.message
        } finally {
            connection?.disconnect()
        }
    }

    private fun runPostScript(
        context: Context,
        item: DownloadNode,
        view: ListItemDownload,
        destFile: File,
        onFinished: () -> Unit
    ) {
        val script = item.setState
        if (script.isNullOrEmpty()) {
            // Không khai báo script sau khi tải xong -> coi như xong ngay.
            view.showStatusLabel(context.getString(R.string.kr_download_execute_success))
            finishAfterDelay(view, onFinished)
            return
        }

        view.showStatusLabel(context.getString(R.string.kr_download_execute_wait))

        val errorRows = ArrayList<String>()
        val handler = object : ShellHandlerBase(context.applicationContext) {
            override fun updateLog(msg: SpannableString) {}
            override fun onError(msg: Any?) {
                synchronized(errorRows) { errorRows.add("" + msg?.toString()) }
            }
            override fun onExit(msg: Any?) {
                val success = errorRows.isEmpty()
                view.showStatusLabel(
                    context.getString(if (success) R.string.kr_download_execute_success else R.string.kr_download_execute_fail)
                )
                finishAfterDelay(view, onFinished)
            }
            override fun onStart(forceStop: Runnable?) {}
            override fun onStart(msg: Any?) {}
            override fun onProgress(current: Int, total: Int) {}
        }

        // $state trong script = đường dẫn tuyệt đối của file vừa tải xong (xem lớp DownloadNode).
        ShellExecutor().execute(context, item, script, null, hashMapOf("state" to destFile.absolutePath), handler)
    }
}
