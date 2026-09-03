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
 * Ví dụ gọi từ shell (banner thường, tự ẩn sau vài giây):
 * am broadcast -a com.tool.tree.broadcast.BANNER \
 *     --es title "Thành công" \
 *     --es text "Đã cài đặt module xong" \
 *     --es type "success" \
 *     --es position "bottom" \
 *     --es icon "ic_my_custom_icon"
 *
 * Ví dụ gọi kèm script cần xác nhận trước khi chạy (hiện thêm 2 nút Xác nhận/Hủy bỏ +
 * đếm ngược, hết giờ mà chưa bấm gì thì tự Hủy bỏ, KHÔNG chạy script):
 * am broadcast -a com.tool.tree.broadcast.BANNER \
 *     --es text "Phát hiện script mới, chạy ngay?" \
 *     --es script "sh /sdcard/Download/update.sh" \
 *     --es confirm "Chạy" \
 *     --es cancel "Bỏ qua" \
 *     --ei countdown 5
 *
 * Extra "type" nhận 1 trong: info (mặc định) | success | warning | error
 * Extra "position" nhận 1 trong: top (mặc định) | bottom
 * Extra "icon" (tùy chọn): có thể là
 *   - đường dẫn file ảnh trên máy, vd "/sdcard/Download/icon.png"
 *   - hoặc tên resource drawable/mipmap có sẵn trong app, vd "ic_banner_success"
 * Bỏ trống hoặc không tìm thấy -> mặc định dùng icon của chính app.
 * Extra "script" (tùy chọn): lệnh/script sẽ được chạy bằng đúng quyền hiện có của app (không
 * tự xin root) khi người dùng bấm nút Xác nhận. Có extra này thì banner mới hiện 2 nút.
 * Extra "confirm" / "cancel" (tùy chọn): nhãn tùy chỉnh cho 2 nút, mặc định "Xác nhận"/"Hủy bỏ".
 * Extra "countdown" (tùy chọn, số nguyên): số giây trước khi banner tự ẩn, mặc định 5. Áp
 * dụng cho CẢ banner thường (tự ẩn) lẫn banner có script (hết giờ = tự Hủy bỏ, không chạy).
 * Nếu app đang ở background (không có Activity foreground), sẽ tự rơi về hiện Toast thường
 * (trường hợp này KHÔNG hỗ trợ nút Xác nhận/Hủy bỏ, script sẽ không được chạy).
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
        val script = intent.getStringExtra("script")
        val confirm = intent.getStringExtra("confirm")
        val cancel = intent.getStringExtra("cancel")
        val countdown = intent.getIntExtra("countdown", 5)

        BannerNotificationManager.show(
            title = title,
            message = message,
            type = type,
            position = position,
            icon = icon,
            script = script,
            confirmText = confirm,
            cancelText = cancel,
            countdownSeconds = countdown,
            onNoActivity = {
                // App đang ở background, không có nơi để hiện banner -> fallback Toast
                // (Toast không có nút, script trong trường hợp này sẽ KHÔNG được chạy)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}