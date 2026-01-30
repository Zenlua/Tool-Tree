package com.tool.tree

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.tool.tree.MainActivity
import com.tool.tree.ThemeConfig
import com.tool.tree.R
import android.os.Process
import android.app.ActivityManager

class WakeLockService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isWakeLockActive = false
    private val WAKE_LOCK_TAG get() = "com.tool.tree.WAKE_LOCK"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ENABLE_WAKELOCK -> enableWakeLock()
            ACTION_DISABLE_WAKELOCK -> disableWakeLock()
            ACTION_END_WAKELOCK -> endWakeLock()
            ACTION_STOP_SERVICE -> stopWakeLockAndService()
        }
        return START_STICKY
    }

    private fun enableWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        }
    
        if (!isWakeLockActive) {
            wakeLock?.acquire()
            isWakeLockActive = true
            startForeground(1, buildNotification())
        }
    }
    
    private fun disableWakeLock() {
        if (isWakeLockActive) {
            wakeLock?.release()
            isWakeLockActive = false
            startForeground(1, buildNotification())
        }
    }

    private fun endWakeLock() {
            stopForeground(true)
            stopSelf()
        }

    private fun stopWakeLockAndService() {
        wakeLock?.release()
        wakeLock = null
        isWakeLockActive = false
        stopForeground(true)
        stopSelf()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appTasks = activityManager.appTasks
        appTasks.forEach { task ->
            task.finishAndRemoveTask()
        }
        System.exit(0)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.wakelock_service_running),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                setLockscreenVisibility(Notification.VISIBILITY_SECRET)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val wakelockActionText = if (isWakeLockActive) getString(R.string.turn_off_wakelock) else getString(R.string.turn_on_wakelock)
        val action = if (isWakeLockActive) ACTION_DISABLE_WAKELOCK else ACTION_ENABLE_WAKELOCK

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_active_with_wakelock))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(R.mipmap.ic_launcher, getString(R.string.stop), createPendingIntent(ACTION_STOP_SERVICE))
            .addAction(R.mipmap.ic_launcher, wakelockActionText, createPendingIntent(action))
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, WakeLockService::class.java).apply { this.action = action }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getService(this, 0, intent, flags)
    }

    override fun onDestroy() {
        wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    // Xử lý khi swipe khỏi recents
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        wakeLock?.release()
        wakeLock = null
        isWakeLockActive = false
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "WakeLockServiceChannel"
        const val ACTION_ENABLE_WAKELOCK = "com.tool.tree.action.ENABLE_WAKELOCK"
        const val ACTION_DISABLE_WAKELOCK = "com.tool.tree.action.DISABLE_WAKELOCK"
        const val ACTION_END_WAKELOCK = "com.tool.tree.action.END_WAKELOCK"
        const val ACTION_STOP_SERVICE = "com.tool.tree.action.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, WakeLockService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, WakeLockService::class.java))
        }
    }
}