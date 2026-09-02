package com.tool.tree

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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

        private val messageHistory = ConcurrentHashMap<Int, MutableList<String>>()
        private const val MAX_HISTORY_ROWS = 12

        private val largeIconCache = ConcurrentHashMap<Int, Bitmap>()
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

        currentNotificationId = intent.getIntExtra("notificationId", 1001)

        if (intent.getBooleanExtra("stop", false)) {
            startForegroundCompat(createProgressBuilder(
                applicationInfo.loadLabel(packageManager).toString(), -1, 0, null, null
            ).build())
            stopWatching()
            manager.cancel(currentNotificationId)
            stopForegroundCompat(remove = true)
            messageHistory.remove(currentNotificationId)
            largeIconCache.remove(currentNotificationId)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent.getBooleanExtra("isError", false)) {
            val title = intent.getStringExtra("title") ?: getString(R.string.channel_name)
            val errorText = intent.getStringExtra("text") ?: getString(R.string.kr_download_error)
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setLargeIcon(resolveLargeIcon(intent))
                .setContentTitle(title)
                .setContentText(errorText)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(buildMessagingStyle(title, errorText, addAsNewMessage = true))
            startForegroundCompat(builder.build())
            stopForegroundCompat(remove = false)
            manager.notify(currentNotificationId, builder.build())
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent.getStringExtra("title")
        val displayTitle = if (title.isNullOrEmpty()) {
            applicationInfo.loadLabel(packageManager).toString()
        } else {
            title
        }

        val customText = intent.getStringExtra("text")

        if (intent.hasExtra("progress")) {
            val progress = intent.getIntExtra("progress", 0)
            val max = intent.getIntExtra("max", 100)
            val downloadedBytes = intent.getLongExtra("downloadedBytes", -1L)
            val totalBytes = intent.getLongExtra("totalBytes", -1L)
            val speedBps = intent.getDoubleExtra("speedBps", 0.0)
            val builder = createProgressBuilder(
                displayTitle, progress, max, customText, resolveLargeIcon(intent),
                downloadedBytes, totalBytes, speedBps
            )
            startForegroundCompat(builder.build())
            return START_NOT_STICKY
        }

        intent.getStringExtra("path")?.let {
            startForegroundCompat(createProgressBuilder(displayTitle, -1, 0, null, resolveLargeIcon(intent)).build())
            startWatching(it, displayTitle)
        }

        return START_NOT_STICKY
    }

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

    private fun resolveLargeIcon(intent: Intent): Bitmap {
        val fromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("largeIcon", Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("largeIcon") as? Bitmap
        }
        if (fromIntent != null) {
            largeIconCache[currentNotificationId] = fromIntent
            return fromIntent
        }
        return largeIconCache[currentNotificationId] ?: drawableToBitmap(packageManager.getApplicationIcon(applicationInfo)).also {
            largeIconCache[currentNotificationId] = it
        }
    }

    private fun buildProgressText(percent: Int, downloadedBytes: Long, totalBytes: Long, speedBps: Double): String {
        val parts = ArrayList<String>(3)
        if (percent >= 0) parts.add("$percent%")
        if (downloadedBytes >= 0) {
            parts.add(
                if (totalBytes > 0) {
                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
                } else {
                    formatBytes(downloadedBytes)
                }
            )
        }
        if (speedBps > 0) parts.add(formatSpeed(speedBps))
        return if (parts.isEmpty()) getString(R.string.processing) else parts.joinToString(" • ")
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.0f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        return String.format("%.2f GB", mb / 1024.0)
    }

    private fun formatSpeed(speedBps: Double): String {
        val mbps = speedBps / (1024.0 * 1024.0)
        return if (mbps >= 0.1) String.format("%.1f MB/s", mbps) else String.format("%.0f KB/s", speedBps / 1024.0)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val targetSize = 256
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, targetSize, targetSize, true)
        }
        val bitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createProgressBuilder(
        title: String,
        progress: Int,
        max: Int,
        customText: String?,
        largeIcon: Bitmap?,
        downloadedBytes: Long = -1L,
        totalBytes: Long = -1L,
        speedBps: Double = 0.0
    ): NotificationCompat.Builder {
        // Progress notifications must NOT use MessagingStyle: that style overrides
        // setContentText() and hides the progress bar, which froze the text at the
        // last event line (e.g. "File download started"). Use plain content + BigText
        // so percent / size / speed and the progress bar are always visible.
        val hasBar = progress >= 0 && max > 0
        val percent = if (hasBar) ((progress * 100f) / max).toInt() else -1
        val displayText = when {
            customText != null -> customText
            hasBar -> buildProgressText(percent, downloadedBytes, totalBytes, speedBps)
            downloadedBytes >= 0 -> buildProgressText(-1, downloadedBytes, totalBytes, speedBps)
            else -> getString(R.string.processing)
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayText))

        // if (largeIcon != null) {
            // builder.setLargeIcon(largeIcon)
        // }

        if (hasBar) {
            builder.setProgress(max, progress, false)
        } else {
            builder.setProgress(0, 0, true)
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

                val largeIcon = largeIconCache[currentNotificationId]
                    ?: drawableToBitmap(packageManager.getApplicationIcon(applicationInfo)).also {
                        largeIconCache[currentNotificationId] = it
                    }

                val builder = when {
                    line == "-1" -> {
                        createProgressBuilder(title, -1, 0, null, largeIcon)
                    }
                    line.contains("/") -> {
                        val parts = line.split("/")
                        if (parts.size == 2) {
                            val p = parts[0].toIntOrNull() ?: return
                            val m = parts[1].toIntOrNull() ?: return
                            createProgressBuilder(title, p, m, null, largeIcon)
                        } else return
                    }
                    else -> {
                        val p = line.toIntOrNull() ?: return
                        createProgressBuilder(title, p, 100, null, largeIcon)
                    }
                }
                manager.notify(currentNotificationId, builder.build())
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?) = null
}
