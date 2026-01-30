package com.tool.tree

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class NotiService : Service() {
    private val CHANNEL_ID = "notification_id"
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    // Xóa thông báo
    private fun deleteNotification(id: Int) {
        notificationManager?.cancel(id)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val message = intent?.getStringExtra("message")
        val title = intent?.getStringExtra("title") ?: getString(R.string.app_name) // Sử dụng tên app nếu không có title
        val id = intent?.getIntExtra("id", 1) ?: 1

        // Kiểm tra nếu muốn xóa thông báo
        if ("true" == intent?.getStringExtra("delete")) {
            deleteNotification(id)
            return START_NOT_STICKY
        } else if (message != null) {
            showNotification(id, message, title)
        }

        return START_NOT_STICKY
    }

    // Hiển thị thông báo
    private fun showNotification(id: Int, message: String, title: String) {
        // Tạo Notification Channel nếu cần (Android O trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Notification",
                NotificationManager.IMPORTANCE_HIGH // Đặt độ ưu tiên cao để thông báo có thể hiện thị ở chế độ Heads-Up
            ).apply {
                // Thiết lập thông tin cho Notification Channel
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
                importance = NotificationManager.IMPORTANCE_HIGH // Đảm bảo rằng thông báo có độ ưu tiên cao
            }

            notificationManager?.createNotificationChannel(notificationChannel)
        }

        // Tạo Notification Builder
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        builder.apply {
            setContentTitle(title)  // Tiêu đề là tên app mặc định
            setContentText(message)
            setStyle(Notification.BigTextStyle().bigText(message))
            setSmallIcon(applicationInfo.icon)  // Sử dụng biểu tượng mặc định của ứng dụng
            setAutoCancel(true)
            setPriority(Notification.PRIORITY_HIGH)  // Đặt độ ưu tiên cao để hiển thị heads-up notification
        }

        // Hiển thị thông báo
        notificationManager?.notify(id, builder.build())
    }
}