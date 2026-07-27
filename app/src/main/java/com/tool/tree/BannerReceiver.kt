package com.tool.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.omarea.common.ui.BannerNotificationManager
import com.omarea.common.ui.BannerType
import com.omarea.krscript.config.StringResRef

/**
 * Nhận lệnh am broadcast để hiện banner thông báo ở trên cùng ứng dụng.
 *
 * Ví dụ gọi từ shell:
 * am broadcast -a com.tool.tree.broadcast.BANNER \
 *     --es title "Thành công" \
 *     --es text "Đã cài đặt module xong" \
 *     --es type "success" \
 *     --el duration 4000
 *
 * Extra "type" nhận 1 trong: info (mặc định) | success | warning | error
 * Extra "duration" (ms) không bắt buộc, mặc định 3000
 * Nếu app đang ở background (không có Activity foreground), sẽ tự rơi về hiện Toast thường.
 */
class BannerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rawText = intent.getStringExtra("text") ?: return
        val message = StringResRef.resolve(context, rawText)
        val title = intent.getStringExtra("title")?.let { StringResRef.resolve(context, it) }
        val type = when (intent.getStringExtra("type")?.lowercase()) {
            "success" -> BannerType.SUCCESS
            "warning" -> BannerType.WARNING
            "error" -> BannerType.ERROR
            else -> BannerType.INFO
        }
        val duration = if (intent.hasExtra("duration")) intent.getLongExtra("duration", 3000L) else 3000L

        BannerNotificationManager.show(
            title = title,
            message = message,
            type = type,
            durationMs = duration,
            onNoActivity = {
                // App đang ở background, không có nơi để hiện banner -> fallback Toast
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
