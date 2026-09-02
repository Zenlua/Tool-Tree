package com.tool.tree

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.ConcurrentHashMap

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "download_channel"
        // Notification IDs được truyền từ DownloadTaskHelper (2000-2999)

        private val messageHistory = ConcurrentHashMap<Int, MutableList<String>>()
        private const val MAX_HISTORY_ROWS = 12
    }

    private lateinit var manager: NotificationManager
    private var currentNotificationId: Int = 1001
    private var observer: FileObserver? = null
    private var lastProgress = -1

    override fun onCreate() {
        super.onCreate()

        manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        // Lấy notification ID từ intent (DownloadTaskHelper truyền vào)
        currentNotificationId = intent.getIntExtra("notificationId", 1001)

        // STOP - xoá notification
        if (intent.getBooleanExtra("stop", false)) {
            // QUAN TRỌNG: mọi lệnh gọi tới service này đều đến từ
            // Context.startForegroundService() (xem DownloadTaskHelper) - kể cả khi mục đích
            // chỉ là DỪNG service. Android 8+ vẫn bắt buộc startForeground() phải được gọi
            // trong vòng vài giây sau đó, nếu không hệ thống tự ném
            // ForegroundServiceDidNotStartInTimeException và CRASH CẢ APP - dù ta không hề
            // định giữ service chạy lâu. Gọi startForeground() rồi ngay lập tức
            // stopForeground()+stopSelf() là cách an toàn để "trả nợ" hợp đồng này.
            startForegroundCompat(createProgressBuilder(
                applicationInfo.loadLabel(packageManager).toString(), -1, 0, null
            ).build())
            stopWatching()
            manager.cancel(currentNotificationId)
            stopForegroundCompat(remove = true)
            messageHistory.remove(currentNotificationId)
            stopSelf()
            return START_NOT_STICKY
        }

        // ERROR - giữ notification, không ongoing, hiển thị lỗi
        if (intent.getBooleanExtra("isError", false)) {
            val title = intent.getStringExtra("title") ?: getString(R.string.channel_name)
            val errorText = intent.getStringExtra("text") ?: getString(R.string.kr_download_error)
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(errorText)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(false) // Giữ notification lỗi
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(buildMessagingStyle(title, errorText, addAsNewMessage = true))
            // Cùng lý do như nhánh "stop" ở trên: phải vào foreground trước rồi mới thoát.
            startForegroundCompat(builder.build())
            // Dừng foreground nhưng KHÔNG xoá notification
            stopForegroundCompat(remove = false)
            manager.notify(currentNotificationId, builder.build())
            stopSelf()
            return START_NOT_STICKY
        }

        // TITLE
        val title = intent.getStringExtra("title")
        val displayTitle = if (title.isNullOrEmpty()) {
            applicationInfo.loadLabel(packageManager).toString()
        } else {
            title
        }

        val customText = intent.getStringExtra("text")

        // Progress từ intent
        if (intent.hasExtra("progress")) {
            val progress = intent.getIntExtra("progress", 0)
            val max = intent.getIntExtra("max", 100)
            val builder = createProgressBuilder(displayTitle, progress, max, customText)
            // QUAN TRỌNG: đây là dòng thật sự đưa service vào trạng thái foreground mà
            // startForegroundService() đòi hỏi - bản cũ chỉ gọi manager.notify() (thông báo
            // thường), KHÔNG đưa service vào foreground. Thiếu dòng này, trong vòng vài giây
            // kể từ lần startForegroundService() đầu tiên, Android sẽ tự crash app bằng
            // ForegroundServiceDidNotStartInTimeException - và vì app chết ngay lúc đó, thông
            // báo "đang tải" (setOngoing(true)) vừa hiện sẽ bị TREO VĨNH VIỄN vì không còn ai
            // sống để cập nhật nó sang lỗi hay xoá nó đi.
            startForegroundCompat(builder.build())
            return START_NOT_STICKY
        }

        // Theo dõi file (tính năng cũ, giữ lại)
        intent.getStringExtra("path")?.let {
            // Cùng lý do trên: phải vào foreground trước khi bắt đầu theo dõi file.
            startForegroundCompat(createProgressBuilder(displayTitle, -1, 0, null).build())
            startWatching(it, displayTitle)
        }

        return START_NOT_STICKY
    }

    /**
     * Gọi startForeground() thật sự (không chỉ manager.notify()) - đây là lệnh "trả nợ" hợp
     * đồng startForegroundService(), bắt buộc trên Android 8+ để tránh
     * ForegroundServiceDidNotStartInTimeException. Trên Android 10+ (Q), truyền kèm
     * foregroundServiceType để khớp với foregroundServiceType="dataSync" đã khai trong
     * manifest - bắt buộc trên Android 14+, nếu không sẽ bị
     * MissingForegroundServiceTypeException/InvalidForegroundServiceTypeException.
     * An toàn gọi lại nhiều lần (mỗi lần cập nhật tiến độ) - Android chỉ cập nhật notification,
     * không lỗi gì khi service đã ở trạng thái foreground từ trước.
     */
    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(currentNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(currentNotificationId, notification)
        }
    }

    private fun buildMessagingStyle(title: String, line: String, addAsNewMessage: Boolean): NotificationCompat.MessagingStyle {
        val rows = messageHistory.getOrPut(currentNotificationId) { java.util.Collections.synchronizedList(ArrayList()) }
        synchronized(rows) {
            if (addAsNewMessage) {
                rows.add(line)
                while (rows.size > MAX_HISTORY_ROWS) rows.removeAt(0)
            } else if (rows.isEmpty()) {
                rows.add(line)
            }
        }

        val person = Person.Builder().setName(title).build()
        val messagingStyle = NotificationCompat.MessagingStyle(person)
        val snapshot = synchronized(rows) { rows.toList() }
        snapshot.forEach { row ->
            messagingStyle.addMessage(row, System.currentTimeMillis(), person)
        }
        return messagingStyle
    }

    private fun createProgressBuilder(title: String, progress: Int, max: Int, customText: String?): NotificationCompat.Builder {
        val isEvent = customText != null
        val displayText = if (progress < 0 || max <= 0) {
            customText ?: getString(R.string.processing)
        } else {
            val percent = ((progress * 100f) / max).toInt()
            customText ?: "$percent%"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(displayText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(true)
            .setUsesChronometer(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(buildMessagingStyle(title, displayText, addAsNewMessage = isEvent))

        if (progress < 0 || max <= 0) {
            builder.setProgress(0, 0, true)
        } else {
            builder.setProgress(max, progress, false)
        }

        return builder
    }

    private fun startWatching(filePath: String, title: String) {
        stopWatching()

        val file = File(filePath)
        val parent = file.parentFile ?: return

        observer = createFileObserver(parent, FileObserver.MODIFY or FileObserver.CREATE) { name ->
            if (name == file.name) {
                readProgress(file, title)
            }
        }

        observer?.startWatching()
        readProgress(file, title)
    }

    private fun createFileObserver(parent: File, mask: Int, onEvent: (String?) -> Unit): FileObserver {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(parent, mask) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    onEvent(path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            object : FileObserver(parent.absolutePath, mask) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == null) return
                    onEvent(path)
                }
            }
        }
    }

    private fun stopForegroundCompat(remove: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(if (remove) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(remove)
        }
    }

    private fun stopWatching() {
        observer?.stopWatching()
        observer = null
    }

    private fun readProgress(file: File, title: String) {
        try {
            BufferedReader(FileReader(file)).use { br ->
                val line = br.readLine()?.trim() ?: return

                val builder = when {
                    line == "-1" -> {
                        createProgressBuilder(title, -1, 0, null)
                    }
                    line.contains("/") -> {
                        val parts = line.split("/")
                        if (parts.size == 2) {
                            val p = parts[0].toIntOrNull() ?: return
                            val m = parts[1].toIntOrNull() ?: return
                            createProgressBuilder(title, p, m, null)
                        } else return
                    }
                    else -> {
                        val p = line.toIntOrNull() ?: return
                        createProgressBuilder(title, p, 100, null)
                    }
                }
                manager.notify(currentNotificationId, builder.build())
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?) = null
}