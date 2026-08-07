package com.tool.tree

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.omarea.krscript.config.StringResRef

class NotiService : Service() {
    private val CHANNEL_ID = "notification_id_am"
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    // Xóa thông báo
    private fun deleteNotification(id: Int) {
        notificationManager?.cancel(id)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rawMessage = intent?.getStringExtra("message")
        val rawTitle = intent?.getStringExtra("title")
        val id = intent?.getIntExtra("id", 1) ?: 1

        // Kiểm tra nếu muốn xóa thông báo
        if (intent?.getBooleanExtra("delete", false) == true) {
            deleteNotification(id)
            return START_NOT_STICKY
        } else if (rawMessage != null) {
            // Giải mã tiêu đề và nội dung thông qua StringResRef
            val title = if (rawTitle != null) {
                StringResRef.resolve(this, rawTitle)
            } else {
                getString(R.string.app_name)
            }

            val message = StringResRef.resolve(this, rawMessage)

            showNotification(id, message, title, intent)
        }

        return START_NOT_STICKY
    }

    // Hiển thị thông báo dạng tin nhắn
    private fun showNotification(id: Int, message: String, title: String, intent: Intent) {
        // 1. Tạo Notification Channel nếu cần (Android O trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                "Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }

            notificationManager?.createNotificationChannel(notificationChannel)
        }

        // 2. Tạo Intent để mở lại app khi nhấn vào thông báo
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }

        val contentPendingIntent = launchIntent?.let {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(this, 0, it, flags)
        }

        // 3. Lấy ảnh lớn và ép kích thước về 200x200
        val avatarBitmap = getAvatarBitmap(intent)
        val iconCompat = IconCompat.createWithBitmap(avatarBitmap)

        // 4. Tạo đối tượng Person đại diện cho người gửi
        val sender = Person.Builder()
            .setName(title)
            .setIcon(iconCompat)
            .build()

        // 5. Cấu hình MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .addMessage(message, System.currentTimeMillis(), sender)

        // 6. Xây dựng thông báo bằng NotificationCompat
        val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(applicationInfo.icon)
            setStyle(messagingStyle)
            setLargeIcon(avatarBitmap) // Gắn thêm largeIcon giúp hiển thị ảnh lớn rõ nét
            setAutoCancel(true)
            setPriority(NotificationCompat.PRIORITY_HIGH)

            contentPendingIntent?.let {
                setContentIntent(it)
            }
        }

        // Hiển thị thông báo
        notificationManager?.notify(id, builder.build())
    }

    /**
     * Hàm xử lý lấy Bitmap từ Intent (Hỗ trợ cờ large_icon / icon),
     * nếu không có sẽ lấy icon của App và ép về kích thước 200x200.
     */
    private fun getAvatarBitmap(intent: Intent): Bitmap {
        val targetSize = 200
        var bitmap: Bitmap? = null

        // 1. Lấy từ extra dạng Bitmap direct
        val bitmapExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("large_icon", Bitmap::class.java)
                ?: intent.getParcelableExtra("icon", Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra("large_icon") as? Bitmap)
                ?: (intent.getParcelableExtra("icon") as? Bitmap)
        }

        if (bitmapExtra != null) {
            bitmap = bitmapExtra
        } else {
            // 2. Lấy từ extra dạng ID Resource (Int)
            var resId = intent.getIntExtra("large_icon", 0)
            if (resId == 0) resId = intent.getIntExtra("icon", 0)

            if (resId != 0) {
                bitmap = drawableToBitmap(ContextCompat.getDrawable(this, resId))
            } else {
                // 3. Lấy từ extra dạng String (Đường dẫn File / Uri String)
                val pathOrUri = intent.getStringExtra("large_icon") ?: intent.getStringExtra("icon")
                if (!pathOrUri.isNull_Or_Empty()) {
                    bitmap = loadBitmapFromString(pathOrUri)
                }
            }
        }

        // 4. Mặc định: Nếu không lấy được ảnh nào -> Dùng Icon của App
        if (bitmap == null) {
            val appDrawable = packageManager.getApplicationIcon(applicationInfo)
            bitmap = drawableToBitmap(appDrawable)
        }

        // 5. Luôn ép kích thước về 200x200
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }

    private fun String?.isNull_Or_Empty(): Boolean = this.isNullOrEmpty()

    private fun loadBitmapFromString(source: String): Bitmap? {
        return try {
            // Thử decode dưới dạng File path
            val fileBitmap = BitmapFactory.decodeFile(source)
            if (fileBitmap != null) return fileBitmap

            // Thử decode dưới dạng Uri (ContentProvider hoặc file://)
            val uri = Uri.parse(source)
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap {
        if (drawable == null) {
            return Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        }
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 200
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
