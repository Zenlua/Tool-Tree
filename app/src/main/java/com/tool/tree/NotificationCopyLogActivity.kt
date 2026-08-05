package com.tool.tree

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

/**
 * Activity trong suốt, không hiển thị giao diện — được mở khi người dùng bấm nút "Sao chép log"
 * trên thông báo tiến trình script.
 *
 * Lý do cần một Activity thay vì xử lý thẳng trong BroadcastReceiver: kể từ Android 10, hệ
 * thống có thể âm thầm bỏ qua ClipboardManager.setPrimaryClip() nếu lệnh gọi đến từ 1 tiến
 * trình không ở foreground (ví dụ từ BroadcastReceiver được kích hoạt bởi nút bấm trên thông
 * báo trong khi app đang chạy nền) — Toast báo "đã sao chép" vẫn hiện vì Toast không bị chặn,
 * nhưng nội dung thực tế không được ghi vào clipboard. Mở 1 Activity (dù trong suốt) khiến app
 * thật sự lên foreground trong khoảnh khắc đó, đảm bảo lệnh setPrimaryClip() được hệ thống chấp nhận.
 */
class NotificationCopyLogActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(EXTRA_LOG_TEXT).orEmpty()
        runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("text", text))
        }

        val messageRes = intent?.getIntExtra(EXTRA_TOAST_MESSAGE_RES, R.string.kr_task_notify_copied)
            ?: R.string.kr_task_notify_copied
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()

        finish()
    }

    companion object {
        const val EXTRA_LOG_TEXT = "log_text"
        const val EXTRA_TOAST_MESSAGE_RES = "toast_message_res"
    }
}