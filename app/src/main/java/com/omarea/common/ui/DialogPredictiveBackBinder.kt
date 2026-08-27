package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.view.Window
import androidx.annotation.RequiresApi

/**
 * Đăng ký cử chỉ vuốt-từ-mép (predictive-back của hệ thống, Android 13+/API 33) cho 1 Dialog
 * ĐÃ bind DialogSwipeBackHelper - Dialog có Window RIÊNG, không dùng chung
 * onBackPressedDispatcher của Activity như trang toàn màn hình (xem ActionPage.kt +
 * SwipeBackHelper), nên phải tự đăng ký thẳng vào dialog.window.onBackInvokedDispatcher thay vì
 * onBackPressedDispatcher.addCallback().
 *
 * Không làm gì (no-op, trả về null) trên API < 33 - DialogSwipeBackHelper vẫn hoạt động bình
 * thường qua vuốt tay trực tiếp trên nội dung dialog như trước, chỉ là không có thêm cử chỉ
 * vuốt từ mép màn hình.
 *
 * @return "token" (thật ra là OnBackInvokedCallback, giữ kiểu Any? để field/biến giữ tham chiếu
 * ở nơi gọi không cần biết tới android.window.OnBackInvokedCallback - tránh mọi rủi ro
 * verify/resolve class đó trên thiết bị API < 33) để truyền lại cho unbind() lúc dialog bị
 * đóng/hủy (tránh leak). Null nếu API không hỗ trợ hoặc dialog chưa có window.
 */
object DialogPredictiveBackBinder {
    fun bind(dialog: Dialog, helper: DialogSwipeBackHelper): Any? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val window = dialog.window ?: return null
        return registerOnApi33(window, dialog, helper)
    }

    fun unbind(dialog: Dialog, token: Any?) {
        if (token == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val window = dialog.window ?: return
        unregisterOnApi33(window, token)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerOnApi33(window: Window, dialog: Dialog, helper: DialogSwipeBackHelper): android.window.OnBackInvokedCallback {
        val callback = object : android.window.OnBackAnimationCallback {
            override fun onBackStarted(backEvent: android.window.BackEvent) {
                helper.onSystemBackStarted()
            }

            override fun onBackProgressed(backEvent: android.window.BackEvent) {
                helper.onSystemBackProgress(backEvent.progress)
            }

            override fun onBackCancelled() {
                helper.onSystemBackCancelled()
            }

            override fun onBackInvoked() {
                if (!helper.consumeSystemBackInvoked()) {
                    dialog.dismiss()
                }
            }
        }
        window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            callback
        )
        return callback
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun unregisterOnApi33(window: Window, token: Any) {
        window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(token as android.window.OnBackInvokedCallback)
    }
}
