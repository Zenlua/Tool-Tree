package com.omarea.krscript

import android.content.Context
import com.omarea.krscript.executor.ShellExecutor
import com.omarea.krscript.model.RunnableNode

/**
 * Dùng để chạy shell được kích hoạt từ nút bấm trên thông báo (NotiService, nút btn_execute).
 *
 * Tách riêng khỏi BgTaskThread.kt để tránh đụng vào logic tác vụ nền hiện có:
 * - Không gọi DialogHelper.helpInfo (yêu cầu Activity context, sẽ crash khi gọi từ Service).
 * - Dùng riêng một dải notificationCounter để không đụng ID với BgTaskThread.
 *
 * Log/tiến trình vẫn hiển thị qua đúng cơ chế thông báo của BgTaskThread
 * (BgTaskThread.ServiceShellHandler: MessagingStyle, progress, nút hủy/copy log...).
 */
object NotiShellTaskLauncher {
    // Dải ID riêng, tách khỏi notificationCounter (34050+) của BgTaskThread để không trùng thông báo
    private var notificationCounter = 84050

    fun startTask(
        context: Context,
        script: String,
        nodeInfo: RunnableNode,
        onExit: Runnable = Runnable {}
    ) {
        val applicationContext = context.applicationContext
        notificationCounter += 1

        val handler = BgTaskThread.ServiceShellHandler(applicationContext, nodeInfo, notificationCounter)
        ShellExecutor().execute(
            context,
            nodeInfo,
            script,
            {
                try {
                    onExit.run()
                } catch (ex: Exception) {
                }
            },
            null,
            handler
        )
    }
}