package com.omarea.krscript

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.omarea.common.ui.DialogHelper
import com.omarea.krscript.config.IconPathAnalysis
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode
import com.omarea.krscript.model.ShellHandlerBase
import com.tool.tree.R

class BgTaskThread(private var process: Process) : Thread() {
    override fun run() {
        try {
            process.waitFor()
        } catch (ex: java.lang.Exception) {
        }
    }

    class ServiceShellHandler(private val context: Context, private val runnableNode: RunnableNode, private val notificationID: Int) : ShellHandlerBase(context) {
        private var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        private val notificationTitle = runnableNode.title
        private var notificationMessageRows = ArrayList<String>()
        private var progressCurrent = 0
        private var progressTotal = 0
        private var someIgnored = false
        private var forceStop: Runnable? = null
        private var isFinished = false
        private var STOP_CLICK_ACTION_NAME = context.packageName + ".TaskStop." + "N" + notificationID
        private val stopIntent = PendingIntent.getBroadcast(context, 0, Intent(STOP_CLICK_ACTION_NAME).apply {
            putExtra("id", notificationID)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.hasExtra("id")) {
                    if (intent.getIntExtra("id", 0) == notificationID) {
                        forceStop?.run()
                    }
                }
            }
        }

        // Nút "Sao chép log" — chép toàn bộ log hiện có (thứ tự cũ -> mới) vào clipboard.
        private var COPY_CLICK_ACTION_NAME = context.packageName + ".TaskCopyLog." + "N" + notificationID
        private val copyIntent = PendingIntent.getBroadcast(context, 0, Intent(COPY_CLICK_ACTION_NAME).apply {
            putExtra("id", notificationID)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private val copyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.hasExtra("id") && intent.getIntExtra("id", 0) == notificationID) {
                    copyLogToClipboard()
                }
            }
        }

        private fun copyLogToClipboard() {
            val text = synchronized(notificationMessageRows) { notificationMessageRows.joinToString("") }.trim()
            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("text", text))
                Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show()
            }
        }

        /** Intent mở lại app khi chạm vào thông báo (không tính vùng nút), giống WakeLockService. */
        private fun buildContentPendingIntent(): PendingIntent? {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            } ?: return null
            return PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun drawableToIcon(drawable: Drawable?): Icon? {
            if (drawable == null) return null
            if (drawable is BitmapDrawable) {
                drawable.bitmap?.let { return Icon.createWithBitmap(it) }
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return Icon.createWithBitmap(bitmap)
        }

        private fun updateNotification() {
            if (notificationMessageRows.size > 20) {
                synchronized(notificationMessageRows) {
                    notificationMessageRows.remove(notificationMessageRows.first())
                    someIgnored = true
                }
            }

            val shortLog = notificationMessageRows.lastOrNull()?.trim().orEmpty()

            // Load icon từ runnableNode.iconPath và truyền đầy đủ currentConfigXml, sau đó đổi sang Icon
            val personIcon = if (runnableNode.iconPath.isNotEmpty()) {
                val drawable = IconPathAnalysis().loadLogo(context, runnableNode, false, runnableNode.currentConfigXml)
                drawableToIcon(drawable)
            } else null

            // Sử dụng android.app.Person đúng chuẩn để tránh lỗi Unresolved reference 'Person'
            val sender = android.app.Person.Builder()
                .setName(notificationTitle)
                .setIcon(personIcon)
                .build()

            // Dùng MessagingStyle để hỗ trợ hiển thị icon
            val messagingStyle = Notification.MessagingStyle(sender)

            // Thêm từng dòng log vào MessagingStyle bằng cách sử dụng Notification.MessagingStyle.Message
            val rows = synchronized(notificationMessageRows) { notificationMessageRows.toList() }
            if (someIgnored) {
                messagingStyle.addMessage(Notification.MessagingStyle.Message("……", System.currentTimeMillis(), (null as android.app.Person?)))
            }
            rows.forEach { row ->
                messagingStyle.addMessage(Notification.MessagingStyle.Message(row.trim(), System.currentTimeMillis(), (null as android.app.Person?)))
            }

            val notificationBuilder = Notification.Builder(context, channelId)
                    .setContentTitle(notificationTitle)
                    .setContentText(shortLog)
                    .setSmallIcon(R.drawable.kr_run)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setStyle(messagingStyle)
            
            if (progressTotal != progressCurrent) {
                notificationBuilder.setProgress(progressTotal, progressCurrent, progressTotal < 0)
            }

            // Chạm vào thông báo (ngoài vùng nút) sẽ mở lại app, giống WakeLockService.
            buildContentPendingIntent()?.let { notificationBuilder.setContentIntent(it) }

            // Nút "Hủy bỏ" hiển thị ở hàng dưới cùng do hệ thống tự vẽ (giống WakeLockService).
            if (runnableNode.interruptable && forceStop != null && !isFinished) {
                notificationBuilder.addAction(R.drawable.kr_cancel, context.getString(R.string.btn_cancel), stopIntent)
            }
            // Nút "Sao chép log" — luôn hiện cạnh nút Hủy bỏ, dùng được ở mọi trạng thái.
            notificationBuilder.addAction(R.drawable.kr_copy, context.getString(R.string.btn_copy_output), copyIntent)

            if (!channelCreated) {
                val channel = NotificationChannel(channelId, context.getString(R.string.kr_script_task_notification), NotificationManager.IMPORTANCE_DEFAULT)
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setSound(null, null)
                notificationManager.createNotificationChannel(channel)
            }
            channelCreated = true
            notificationBuilder.setChannelId(channelId)

            val notification = notificationBuilder.build()

            if (!isFinished) {
                notification.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT
            }

            notificationManager.notify(notificationID, notification) // 发送通知
        }

        override fun updateLog(msg: SpannableString?) {
        }

        override fun onReader(msg: Any?) {
            synchronized(notificationMessageRows) {
                notificationMessageRows.add("" + msg?.toString())
                updateNotification()
            }
        }

        override fun onError(msg: Any?) {
            synchronized(notificationMessageRows) {
                notificationMessageRows.add("" + msg?.toString())
                updateNotification()
            }
        }

        override fun onWrite(msg: Any?) {
        }

        override fun onExit(msg: Any?) {
            try {
                // context.unregisterReceiver(receiver)
            } catch (ex: java.lang.Exception) {
            }
            isFinished = true
            synchronized(notificationMessageRows) {
                // Luôn kết thúc bằng "\n" và không có "\n" thừa ở đầu — trước đây dòng này bị
                // dính liền vào dòng log kế tiếp do thiếu dấu xuống dòng ở cuối chuỗi.
                if (msg == 0) {
                    notificationMessageRows.add(context.getString(R.string.kr_shell_completed) + "\n")
                } else {
                    notificationMessageRows.add("${context.getString(R.string.kr_shell_finish_error)} $msg\n")
                }
                updateNotification()
            }
        }

        override fun onStart(forceStop: Runnable?) {
            this.forceStop = forceStop
            // Android 13+ (API 33) bắt buộc cờ RECEIVER_EXPORTED/NOT_EXPORTED, thiếu sẽ ném
            // SecurityException khiến receiver không đăng ký được -> nút dừng trong thông báo vô tác dụng.
            runCatching {
                ContextCompat.registerReceiver(context, receiver, IntentFilter(STOP_CLICK_ACTION_NAME), ContextCompat.RECEIVER_NOT_EXPORTED)
            }
            runCatching {
                ContextCompat.registerReceiver(context, copyReceiver, IntentFilter(COPY_CLICK_ACTION_NAME), ContextCompat.RECEIVER_NOT_EXPORTED)
            }

            updateNotification()
        }

        override fun onStart(msg: Any?) {
        }

        override fun onProgress(current: Int, total: Int) {
            progressCurrent = current
            progressTotal = total
            updateNotification()
        }
    }

    companion object {
        private var channelCreated = false
        private const val channelId = "kr_script_task_notification"
        private var notificationCounter = 34050

        fun startTask(context: Context, script: String, params: HashMap<String, String>?, nodeInfo: RunnableNode, onExit: Runnable, onDismiss: Runnable) {
            val applicationContext = context.applicationContext
            notificationCounter += 1

            val handler = ServiceShellHandler(applicationContext, nodeInfo, notificationCounter)
            ShellExecutor().execute(
                    context,
                    nodeInfo,
                    script,
                    {
                        try {
                            onExit.run()
                            onDismiss.run()
                        } catch (ex: Exception) {
                        }
                    },
                    params,
                    handler)

            val bundle = Bundle()
            params?.run {
                bundle.putSerializable("params", params)
            }
            DialogHelper.helpInfo(context, context.getString(R.string.kr_bg_task_start), context.getString(R.string.kr_bg_task_start_desc))
        }
    }
}
