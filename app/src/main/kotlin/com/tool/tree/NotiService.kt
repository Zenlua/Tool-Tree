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
import com.omarea.krscript.NotiShellTaskLauncher
import com.omarea.krscript.config.StringResRef
import com.omarea.krscript.model.RunnableNode

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
        val id = intent?.getIntExtra("id", 10) ?: 10

        // Kiểm tra nếu muốn xóa thông báo
        if (intent?.getBooleanExtra("delete", false) == true) {
            deleteNotification(id)
            return START_NOT_STICKY
        }

        // Người dùng vừa bấm nút "btn_execute" trên thông báo -> chạy shell đính kèm
        if (intent?.getBooleanExtra("execute", false) == true) {
            executeShell(intent, id)
            return START_NOT_STICKY
        }

        val rawMessage = intent?.getStringExtra("message")
        val rawTitle = intent?.getStringExtra("title")

        if (rawMessage != null) {
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

    // Chạy shell đính kèm khi cờ "shell" được cấp, log tiến trình thông qua cơ chế thông báo của BgTaskThread
    private fun executeShell(intent: Intent, id: Int) {
        val shell = intent.getStringExtra("shell")
        if (shell.isNullOrEmpty()) {
            return
        }

        val rawTitle = intent.getStringExtra("title")
        val title = if (rawTitle != null) {
            StringResRef.resolve(this, rawTitle)
        } else {
            getString(R.string.app_name)
        }

        // Đóng thông báo gốc, tác vụ đang chạy sẽ có thông báo tiến trình riêng do BgTaskThread quản lý
        deleteNotification(id)

        val nodeInfo = RunnableNode("").apply {
            this.title = title
            this.shell = RunnableNode.shellModeBgTask
            this.interruptable = true
        }

        NotiShellTaskLauncher.startTask(
            applicationContext,
            shell,
            nodeInfo
        )
    }

    // Tạo PendingIntent cho nút "btn_execute", bấm vào sẽ gửi lại cờ "execute" cùng nội dung shell để chạy
    private fun buildExecutePendingIntent(id: Int, rawTitle: String?, shell: String): PendingIntent {
        val executeIntent = Intent(this, NotiService::class.java).apply {
            putExtra("execute", true)
            putExtra("id", id)
            putExtra("title", rawTitle)
            putExtra("shell", shell)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        // requestCode dùng id để tránh đè PendingIntent giữa các thông báo khác nhau
        return PendingIntent.getService(this, id, executeIntent, flags)
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
            setSmallIcon(R.drawable.tab_favorites)
            setStyle(messagingStyle)
            setLargeIcon(avatarBitmap) // Gắn thêm largeIcon giúp hiển thị ảnh lớn rõ nét
            setAutoCancel(true)
            setPriority(NotificationCompat.PRIORITY_HIGH)

            contentPendingIntent?.let {
                setContentIntent(it)
            }
        }

        // Chỉ hiện nút xác nhận chạy shell (btn_execute) khi thông báo có cờ "shell"
        val shell = intent.getStringExtra("shell")
        if (!shell.isNullOrEmpty()) {
            val executePendingIntent = buildExecutePendingIntent(id, intent.getStringExtra("title"), shell)
            builder.addAction(R.drawable.kr_run, getString(R.string.btn_execute), executePendingIntent)
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
                if (!pathOrUri.isNullOrEmpty()) {
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