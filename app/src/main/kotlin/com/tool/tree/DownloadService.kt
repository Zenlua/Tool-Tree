package com.tool.tree

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "download_channel"
        // Notification IDs được truyền từ DownloadTaskHelper (2000-2999)
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
            stopWatching()
            manager.cancel(currentNotificationId)
            stopForegroundCompat(remove = true)
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
            manager.notify(currentNotificationId, builder.build())
            return START_NOT_STICKY
        }

        // Theo dõi file (tính năng cũ, giữ lại)
        intent.getStringExtra("path")?.let {
            startWatching(it, displayTitle)
        }

        return START_NOT_STICKY
    }

    private fun createProgressBuilder(title: String, progress: Int, max: Int, customText: String?): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(true)
            .setUsesChronometer(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress < 0 || max <= 0) {
            // Đang xử lý không xác định
            builder.setProgress(0, 0, true)
                .setContentText(customText ?: getString(R.string.processing))
        } else {
            val percent = ((progress * 100f) / max).toInt()
            val text = customText ?: "$percent%"
            builder.setProgress(max, progress, false)
                .setContentText(text)
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