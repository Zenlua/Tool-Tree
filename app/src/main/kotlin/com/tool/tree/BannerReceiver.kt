package com.tool.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.omarea.common.ui.BannerNotificationManager
import com.omarea.common.ui.BannerPosition
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
 *     --es position "bottom" \
 *     --es icon "ic_my_custom_icon"
 *
 * Extra "type" nhận 1 trong: info (mặc định) | success | warning | error
 * Extra "position" nhận 1 trong: top (mặc định) | bottom
 * Extra "icon" (tùy chọn): có thể là
 *   - đường dẫn file ảnh trên máy, vd "/sdcard/Download/icon.png"
 *   - hoặc tên resource drawable/mipmap có sẵn trong app, vd "ic_banner_success"
 * Bỏ trống hoặc không tìm thấy -> mặc định dùng icon của chính app.
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
        val position = when (intent.getStringExtra("position")?.lowercase()) {
            "top" -> BannerPosition.TOP
            else -> BannerPosition.BOTTOM
        }
        val icon = intent.getStringExtra("icon")

        BannerNotificationManager.show(
            title = title,
            message = message,
            type = type,
            position = position,
            icon = icon,
            onNoActivity = {
                // App đang ở background, không có nơi để hiện banner -> fallback Toast
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}