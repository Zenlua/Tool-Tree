package com.tool.tree

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
        handleIntent(intent)
    }

    // android:launchMode="singleInstance" khiến 1 instance của Activity này có thể được tái sử
    // dụng cho lần bấm nút tiếp theo thay vì tạo instance mới — khi đó hệ thống gọi onNewIntent()
    // chứ không phải onCreate(). Trước đây hàm này không được override nên nếu người dùng bấm
    // nút "Sao chép log" một lần nữa trong lúc instance cũ chưa kịp finish() (ví dụ máy đang bận,
    // Doze, hoặc thao tác rất nhanh), lần bấm đó sẽ không copy gì cả và có thể để lại 1 Activity
    // trong suốt còn sống sót trong task stack.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val text = intent?.getStringExtra(EXTRA_LOG_TEXT).orEmpty()

        val messageRes = when {
            text.isBlank() -> {
                // Log rỗng (ví dụ tác vụ vừa mới bắt đầu, chưa có output): không ghi đè
                // clipboard bằng chuỗi rỗng — người dùng có thể đang có nội dung hữu ích khác
                // trong clipboard, việc xóa mất nó không mang lại lợi ích gì.
                R.string.kr_task_notify_copy_empty
            }
            else -> {
                val copied = runCatching {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                }.isSuccess
                // Trước đây runCatching nuốt lỗi âm thầm rồi vẫn báo "đã sao chép" dù thao tác
                // thất bại (ví dụ ClipboardManager null hoặc bị chặn bởi OEM); giờ phân biệt rõ
                // 2 trường hợp để Toast phản ánh đúng kết quả thực tế.
                if (copied) R.string.copy_success else R.string.copy_fail
            }
        }
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()

        finish()
    }

    companion object {
        const val EXTRA_LOG_TEXT = "log_text"
    }
}