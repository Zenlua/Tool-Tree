package com.tool.tree

import android.app.*
import android.content.Intent
import android.os.*
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DownloadService : Service() {

    private val channelId = "download_channel"
    private val notificationId = 1001

    private lateinit var manager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder
    private var observer: FileObserver? = null
    private var lastProgress = -1

    override fun onCreate() {
        super.onCreate()

        manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setShowWhen(true)
            .setUsesChronometer(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            
        builder.setContentTitle(getString(R.string.channel_name))

        startForeground(notificationId, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent ?: return START_NOT_STICKY

        // STOP
        if (intent.getBooleanExtra("stop", false)) {
            stopWatching()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        // TITLE
        var title = intent.getStringExtra("title")
        if (title.isNullOrEmpty()) {
            title = applicationInfo.loadLabel(packageManager).toString()
        }
        builder.setContentTitle(title)

        val customText = intent.getStringExtra("text")

        // Progress từ intent
        if (intent.hasExtra("progress")) {
            val progress = intent.getIntExtra("progress", 0)
            val max = intent.getIntExtra("max", 100)
            updateProgress(progress, max, customText)
        }

        // Theo dõi file
        intent.getStringExtra("path")?.let {
            startWatching(it)
        }

        if (!intent.hasExtra("progress")) {
            manager.notify(notificationId, builder.build())
        }
        return START_NOT_STICKY
    }

    private fun updateProgress(progress: Int, max: Int, customText: String?) {
        if (progress == lastProgress) return
        lastProgress = progress
        if (progress < 0 || max <= 0) {
            // Đang xử lý không xác định
            builder.setProgress(0, 0, true)
                .setContentText(customText ?: getString(R.string.processing))
    
            manager.notify(notificationId, builder.build())
            return
        }
    
        val percent = ((progress * 100f) / max).toInt()
        val text = customText ?: "$percent%"
    
        if (progress >= max) {
            stopWatching()
        
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setContentText(getString(R.string.completed))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
        
            manager.notify(notificationId, builder.build())
        
            stopForeground(false)
            stopSelf()
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(max, progress, false)
                .setContentText(text)
    
            manager.notify(notificationId, builder.build())
        }
    }

    private fun startWatching(filePath: String) {

        stopWatching()

        val file = File(filePath)
        val parent = file.parentFile ?: return

        observer = object : FileObserver(
            parent.absolutePath,
            MODIFY or CREATE
        ) {
            override fun onEvent(event: Int, name: String?) {
                if (name == null) return
                if (name == file.name) {
                    readProgress(file)
                }
            }
        }

        observer?.startWatching()
        readProgress(file)
    }

    private fun stopWatching() {
        observer?.stopWatching()
        observer = null
    }

    private fun readProgress(file: File) {
        try {
            BufferedReader(FileReader(file)).use { br ->
                val line = br.readLine()?.trim() ?: return

                when {
                    line == "-1" -> updateProgress(-1, 0, null)

                    line.contains("/") -> {
                        val parts = line.split("/")
                        if (parts.size == 2) {
                            val p = parts[0].toIntOrNull() ?: return
                            val m = parts[1].toIntOrNull() ?: return
                            updateProgress(p, m, null)
                        }
                    }

                    else -> {
                        val p = line.toIntOrNull() ?: return
                        updateProgress(p, 100, null)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?) = null
}