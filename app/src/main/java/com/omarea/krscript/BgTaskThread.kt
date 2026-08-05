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
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.omarea.common.ui.DialogHelper
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

        private fun updateNotification() {
            if (notificationMessageRows.size > 20) {
                synchronized(notificationMessageRows) {
                    notificationMessageRows.remove(notificationMessageRows.first())
                    someIgnored = true
                }
            }

            val shortLog = notificationMessageRows.lastOrNull()?.trim().orEmpty()
            // Log đầy đủ, MỚI NHẤT LÊN TRÊN CÙNG để nếu bị Android cắt bớt do quá dài (khi mở
            // rộng) thì phần bị cắt luôn là log CŨ, còn log mới nhất luôn được giữ lại.
            val fullLog = run {
                val rows = synchronized(notificationMessageRows) { notificationMessageRows.toList() }
                val sb = StringBuilder()
                for (i in rows.indices.reversed()) sb.append(rows[i])
                if (someIgnored) sb.append("……\n")
                sb.toString().trim()
            }

            val notificationBuilder = Notification.Builder(context, channelId)
                    .setContentTitle(notificationTitle)
                    .setContentText(shortLog)
                    .setSmallIcon(R.drawable.kr_run)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    // Dùng style mặc định của Android: hệ thống tự vẽ mũi tên mở rộng và cho
                    // phép ấn giữ/vuốt để xem toàn bộ log, không cần tự vẽ layout riêng.
                    .setStyle(Notification.BigTextStyle().bigText(fullLog))
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
                        /*
                        try {
                            process.destroy()
                        } catch (ex: java.lang.Exception) {
                        }
                        */
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
            // Toast.makeText(applicationContext, applicationContext.getString(R.string.kr_bg_task_start), Toast.LENGTH_SHORT).show()
        }
    }
}
