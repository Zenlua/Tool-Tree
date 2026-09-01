package com.omarea.krscript.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import androidx.lifecycle.LifecycleCoroutineScope
import com.tool.tree.DownloadService
import com.tool.tree.R
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.DownloadNode
import com.omarea.krscript.model.ShellHandlerBase
import com.omarea.krscript.ui.ListItemDownload
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Xử lý cho loại mục [[download]]: tải file qua HTTP(S) với hỗ trợ:// - Tạm dừng / tiếp tục (HTTP Range / partial content)
// - Thử lại 1 lần khi chuyển đổi mạng
// - Thông báo notification trong suốt quá trình tải (qua DownloadService)
// - Tải không bị gián đoạn khi rời trang (dùng appScope, không lifecycleScope)
// - Tự động xoá notification khi tải xong, giữ notification khi lỗi
//
// Bấm lại item: đang tải → tạm dừng; đang tạm dừng → tiếp tục; lỗi → tải lại.
object DownloadTaskHelper {
    private const val STATUS_HOLD_MS = 900L
    private const val TAG = "DownloadTaskHelper"

    // ────────────── Session ──────────────
    enum class Status { IDLE, DOWNLOADING, PAUSED, COMPLETING, COMPLETED, ERROR }

    class Session(
        val id: String,
        val url: String,
        val destFile: File,
        val title: String,
        val item: DownloadNode,
        // Application context để gửi notification ngay cả khi không có view
        val appContext: Context,
        var downloaded: Long = 0L,
        var total: Long = -1L,
        var status: Status = Status.IDLE,
        var error: String? = null,
        var retriedOnNetworkChange: Boolean = false,
        var connection: HttpURLConnection? = null,
        var job: Job? = null,
        var onFinished: (() -> Unit)? = null
    ) {
        // Weak ref tới view – có thể null nếu trang không hiển thị
        @Volatile var viewRef: ListItemDownload? = null
            private set

        fun bindView(v: ListItemDownload?) { viewRef = v }
    }

    // Application-level scope – không bị hủy khi fragment/page bị destroy
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Danh sách session đang hoạt động, keyed bởi URL
    private val sessions = ConcurrentHashMap<String, Session>()

    // Trả về session đang hoạt động cho URL (nếu có)
    fun getSession(url: String): Session? = sessions[url]

    // ────────────── Bắt đầu tải mới ──────────────
    fun start(
        context: Context,
        scope: LifecycleCoroutineScope,  // chỉ dùng ban đầu để trigger qua UI thread
        item: DownloadNode,
        view: ListItemDownload,
        onFinished: () -> Unit
    ) {
        // Nếu đã có session cho URL này → re-bind view và không tạo mới
        val existing = sessions[item.url]
        if (existing != null && existing.status != Status.COMPLETED && existing.status != Status.ERROR) {
            bindView(existing, view)
            return
        }
        // Xoá session cũ đã kết thúc
        existing?.let { sessions.remove(item.url) }

        val destFile = try {
            File(context.cacheDir, "kr_download_" + UUID.randomUUID().toString().replace("-", "") + guessSuffix(item.url))
        } catch (ex: Exception) {
            view.showStatusLabel(context.getString(R.string.kr_download_error))
            finishAfterDelay(view, onFinished)
            return
        }

        val session = Session(
            id = UUID.randomUUID().toString(),
            url = item.url,
            destFile = destFile,
            title = item.title.ifEmpty { item.url.substringAfterLast('/').substringBefore('?') },
            item = item,
            appContext = context.applicationContext,
            onFinished = onFinished
        )
        sessions[item.url] = session
        bindView(session, view)

        session.job = appScope.launch {
            runDownload(context, session)
        }
    }

    // ────────────── Tạm dừng ──────────────
    fun pause(session: Session) {
        if (session.status != Status.DOWNLOADING) return
        session.status = Status.PAUSED
        try {
            session.connection?.disconnect()
        } catch (_: Exception) {}
        session.connection = null
        postMain {
            session.viewRef?.showStatusLabel(session.viewRef?.context?.getString(R.string.kr_download_paused_tap) ?: "Paused")
        }
        updateNotification(session)
    }

    // ────────────── Tiếp tục ──────────────
    fun resume(context: Context, session: Session) {
        if (session.status != Status.PAUSED) return
        session.status = Status.DOWNLOADING
        session.job = appScope.launch {
            runDownload(context, session)
        }
    }

    // ────────────── Hủy hoàn toàn ──────────────
    fun cancel(session: Session) {
        session.status = Status.IDLE
        session.job?.cancel()
        try { session.connection?.disconnect() } catch (_: Exception) {}
        session.connection = null
        session.destFile.delete()
        sessions.remove(session.url)
        dismissNotification(session)
        postMain {
            session.viewRef?.let { v ->
                v.restoreDesc()
                v.finishBusy()
            }
        }
    }

    // ────────────── Re-bind view khi quay lại trang ──────────────
    fun bindView(session: Session, view: ListItemDownload) {
        postMain {
            session.bindView(view)
            when (session.status) {
                Status.DOWNLOADING -> {
                    view.markBusy { pause(session) }
                    if (session.total > 0) {
                        view.updateDownloadProgress(session.downloaded, session.total)
                    } else {
                        view.showStatusLabel(view.context.getString(R.string.kr_download_execute_wait))
                    }
                }
                Status.PAUSED -> {
                    view.markBusy { resume(view.context, session) }
                    view.showStatusLabel(view.context.getString(R.string.kr_download_paused_tap))
                }
                Status.COMPLETING -> {
                    view.markBusy {}
                    view.showStatusLabel(view.context.getString(R.string.kr_download_execute_wait))
                }
                Status.COMPLETED -> {
                    view.showStatusLabel(view.context.getString(R.string.kr_download_execute_success))
                    view.finishBusy()
                }
                Status.ERROR -> {
                    view.showStatusLabel(view.context.getString(R.string.kr_download_error) + ": " + (session.error ?: ""))
                    // Giữ trạng thái lỗi – không gọi finishBusy()
                }
                Status.IDLE -> {
                    view.restoreDesc()
                    view.finishBusy()
                }
            }
        }
    }

    // ────────────── Core download logic ──────────────
    @SuppressLint("MissingPermission")
    private suspend fun runDownload(context: Context, session: Session) {
        session.status = Status.DOWNLOADING
        postMain { session.viewRef?.markBusy { pause(session) } }
        // Luôn hiện thông báo tải về khi bắt đầu
        startNotification(session)

        // Đăng ký lắng nghe chuyển đổi mạng
        val networkCallback = registerNetworkCallback(context) {
            if (session.status == Status.DOWNLOADING && !session.retriedOnNetworkChange) {
                session.retriedOnNetworkChange = true
                postMain {
                    session.viewRef?.showStatusLabel(
                        session.viewRef?.context?.getString(R.string.kr_download_retrying)
                            ?: "Network changed, retrying…"
                    )
                }
                updateNotification(session, textOverride = context.getString(R.string.kr_download_retrying))
                // Ngắt connection hiện tại để trigger retry trong vòng lặp
                try { session.connection?.disconnect() } catch (_: Exception) {}
                session.connection = null
            }
        }

        try {
            while (session.status == Status.DOWNLOADING) {
                val error = downloadToFile(
                    url = session.url,
                    destFile = session.destFile,
                    existingBytes = session.downloaded,
                    onConnectionOpened = { conn -> session.connection = conn },
                    onProgress = { downloaded, total ->
                        session.downloaded = downloaded
                        session.total = total
                        postMain {
                            val v = session.viewRef
                            if (v != null && session.status == Status.DOWNLOADING) {
                                v.updateDownloadProgress(downloaded, total)
                            }
                        }
                        updateNotification(session)
                    }
                )

                if (session.status == Status.PAUSED) break

                if (error != null) {
                    // Nếu chưa retry vì đổi mạng → thử lại 1 lần
                    if (!session.retriedOnNetworkChange && isNetworkRelatedError(error)) {
                        session.retriedOnNetworkChange = true
                        postMain {
                            session.viewRef?.showStatusLabel(
                                session.viewRef?.context?.getString(R.string.kr_download_retrying)
                                    ?: "Network changed, retrying…"
                            )
                        }
                        updateNotification(session, textOverride = context.getString(R.string.kr_download_retrying))
                        delay(500) // chờ mạng ổn định
                        continue // retry
                    }
                    // Lỗi thực sự
                    session.status = Status.ERROR
                    session.error = error
                    postMain {
                        session.viewRef?.let { v ->
                            v.clearCancelAction()
                            v.showStatusLabel(v.context.getString(R.string.kr_download_error) + ": " + error)
                            // KHÔNG gọi finishBusy() – giữ trạng thái lỗi trên item
                        }
                    }
                    updateNotificationError(session)
                    break
                }

                // Tải xong byte → chạy post-script
                postMain { session.viewRef?.clearCancelAction() }
                session.status = Status.COMPLETING
                updateNotification(session, textOverride = context.getString(R.string.kr_download_execute_wait))
                runPostScript(context, session)
                break
            }
        } finally {
            unregisterNetworkCallback(context, networkCallback)
        }
    }

    // ────────────── HTTP download với hỗ trợ Range (resume) ──────────────
    private fun downloadToFile(
        url: String,
        destFile: File,
        existingBytes: Long,
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

            // Hỗ trợ resume: nếu đã tải được phần nào, dùng Range header
            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
            }

            connection.connect()
            onConnectionOpened(connection)

            val responseCode = connection.responseCode
            // 206 Partial Content = server hỗ trợ resume
            // 200 OK = server không hỗ trợ range, tải lại từ đầu
            if (responseCode !in 200..299 && responseCode != 206) {
                return "HTTP $responseCode"
            }

            val acceptRanges = connection.getHeaderField("Accept-Ranges")
            val contentRange = connection.getHeaderField("Content-Range")
            var total: Long
            var append: Boolean

            if (responseCode == 206 && (acceptRanges == "bytes" || contentRange != null)) {
                // Server hỗ trợ resume
                append = true
                total = parseTotalFromContentRange(contentRange)
                    ?: (connection.contentLengthLong + existingBytes)
            } else {
                // Server trả 200 hoặc không hỗ trợ range → tải lại từ đầu
                append = false
                total = connection.contentLengthLong
            }

            var downloaded = if (append) existingBytes else 0L
            var lastReported = 0L

            connection.inputStream.use { input ->
                RandomAccessFile(destFile, if (append) "rw" else "rw").use { output ->
                    if (append) {
                        output.seek(existingBytes)
                    } else {
                        output.setLength(0)
                    }
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastReported >= 32 * 1024 || lastReported == 0L) {
                            lastReported = downloaded
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
            onProgress(downloaded, total)
            null
        } catch (ex: CancellationException) {
            "cancelled"
        } catch (ex: Exception) {
            "" + ex.message
        } finally {
            connection?.disconnect()
        }
    }

    // Parse tổng dung lượng từ header "Content-Range: bytes START-END/TOTAL"
    private fun parseTotalFromContentRange(contentRange: String?): Long? {
        if (contentRange == null) return null
        val slashIndex = contentRange.lastIndexOf('/')
        if (slashIndex < 0) return null
        return contentRange.substring(slashIndex + 1).trim().toLongOrNull()
    }

    // Kiểm tra xem lỗi có liên quan mạng không
    private fun isNetworkRelatedError(error: String): Boolean {
        val lower = error.lowercase()
        return lower.contains("timeout")
            || lower.contains("socket")
            || lower.contains("network")
            || lower.contains("connection")
            || lower.contains("eof")
            || lower.contains("reset")
            || lower.contains("unreachable")
            || lower.contains("unknownhost")
    }

    // ────────────── Post-script ──────────────
    private fun runPostScript(
        context: Context,
        session: Session
    ) {
        val item = session.item
        val script = item.setState
        if (script.isNullOrEmpty()) {
            completeSession(context, session, success = true)
            return
        }

        postMain {
            session.viewRef?.showStatusLabel(
                session.viewRef?.context?.getString(R.string.kr_download_execute_wait)
                ?: "Running script…"
            )
        }

        val errorRows = ArrayList<String>()
        val handler = object : ShellHandlerBase(context.applicationContext) {
            override fun updateLog(msg: SpannableString) {}
            override fun onError(msg: Any?) {
                synchronized(errorRows) { errorRows.add("" + msg?.toString()) }
            }
            override fun onExit(msg: Any?) {
                val success = errorRows.isEmpty()
                completeSession(context, session, success)
            }
            override fun onStart(forceStop: Runnable?) {}
            override fun onStart(msg: Any?) {}
            override fun onProgress(current: Int, total: Int) {}
        }

        ShellExecutor().execute(context, item, script, null, hashMapOf("state" to session.destFile.absolutePath), handler)
    }

    // ────────────── Kết thúc session ──────────────
    private fun completeSession(context: Context, session: Session, success: Boolean) {
        if (success) {
            session.status = Status.COMPLETED
            postMain {
                session.viewRef?.let { v ->
                    v.showStatusLabel(v.context.getString(R.string.kr_download_execute_success))
                    v.finishBusy()
                }
                session.onFinished?.invoke()
            }
            // Tự động xoá notification khi tải xong
            dismissNotification(session)
            // Xoá session sau một khoảng thời gian
            appScope.launch {
                delay(2000)
                sessions.remove(session.url)
            }
        } else {
            session.status = Status.ERROR
            session.error = "Script failed"
            postMain {
                session.viewRef?.let { v ->
                    v.clearCancelAction()
                    v.showStatusLabel(v.context.getString(R.string.kr_download_execute_fail))
                    // KHÔNG finishBusy() – giữ thông báo lỗi
                }
            }
            updateNotificationError(session)
        }
    }

    // ────────────── Notification ──────────────
    private const val NOTIFICATION_BASE_ID = 2000

    private fun notificationId(session: Session): Int {
        return NOTIFICATION_BASE_ID + session.id.hashCode().mod(1000)
    }

    private fun startNotification(session: Session) {
        val ctx = session.appContext
        val intent = Intent(ctx, DownloadService::class.java).apply {
            putExtra("title", session.title)
            putExtra("progress", 0)
            putExtra("max", 100)
            putExtra("notificationId", notificationId(session))
        }
        ctx.startForegroundService(intent)
    }

    private fun updateNotification(session: Session, textOverride: String? = null) {
        val ctx = session.appContext
        val intent = Intent(ctx, DownloadService::class.java).apply {
            putExtra("title", session.title)
            val progress = if (session.total > 0) (session.downloaded * 100 / session.total).toInt() else -1
            putExtra("progress", progress)
            putExtra("max", 100)
            putExtra("text", textOverride)
            putExtra("notificationId", notificationId(session))
        }
        ctx.startForegroundService(intent)
    }

    private fun updateNotificationError(session: Session) {
        val ctx = session.appContext
        val intent = Intent(ctx, DownloadService::class.java).apply {
            putExtra("title", session.title)
            putExtra("text", ctx.getString(R.string.kr_download_error) + ": " + (session.error ?: ""))
            putExtra("notificationId", notificationId(session))
            putExtra("isError", true)
        }
        ctx.startForegroundService(intent)
    }

    private fun dismissNotification(session: Session) {
        val ctx = session.appContext
        val intent = Intent(ctx, DownloadService::class.java).apply {
            putExtra("stop", true)
            putExtra("notificationId", notificationId(session))
        }
        ctx.startForegroundService(intent)
    }

    // ────────────── Network change listener ──────────────
    private val networkCallbacks = ConcurrentHashMap<Context, ConnectivityManager.NetworkCallback>()

    private fun registerNetworkCallback(
        context: Context,
        onNetworkChanged: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkChanged()
            }
            override fun onLost(network: Network) {
                onNetworkChanged()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        networkCallbacks[context] = callback
        return callback
    }

    private fun unregisterNetworkCallback(context: Context, callback: ConnectivityManager.NetworkCallback) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {}
        networkCallbacks.remove(context)
    }

    // ────────────── Helpers ──────────────
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

    // Gọi từ ActionListFragment để huỷ session theo URL
    fun cancelByUrl(url: String) {
        sessions[url]?.let { cancel(it) }
    }

    // Gọi từ ActionListFragment để tạm dừng session theo URL
    fun pauseByUrl(url: String) {
        sessions[url]?.let { pause(it) }
    }

    // Gọi từ ActionListFragment để tiếp tục session theo URL
    fun resumeByUrl(context: Context, url: String) {
        sessions[url]?.let { resume(context, it) }
    }

    // Kiểm tra URL có session đang hoạt động không
    fun hasActiveSession(url: String): Boolean {
        val s = sessions[url] ?: return false
        return s.status == Status.DOWNLOADING || s.status == Status.PAUSED || s.status == Status.COMPLETING
    }
}