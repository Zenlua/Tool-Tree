package com.tool.tree

import android.app.Activity
import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher

/**
 * Hiệu ứng "vuốt để trở lại" (Predictive Back Gesture - Android 13+/API 33+) cho 1 Activity.
 *
 * Mặc định, dùng `onBackPressedDispatcher.addCallback(this) { ... }` (callback đơn giản)
 * sẽ CHẶN MẤT hiệu ứng preview: hệ thống chỉ gọi hàm xử lý DUY NHẤT một lần khi người dùng
 * đã thả tay xong cử chỉ, giống hệt kiểu Back cũ của AOSP (đóng màn hình đột ngột, không có
 * animation theo tiến trình kéo).
 *
 * Helper này đăng ký một callback "biết" theo dõi toàn bộ tiến trình cử chỉ:
 * - handleOnBackStarted / handleOnBackProgressed: được gọi liên tục trong lúc ngón tay đang
 *   kéo từ cạnh màn hình vào giữa. Ta dùng progress (0f -> 1f) để scale nhẹ nội dung màn hình
 *   hiện tại (100% -> minScale) và dịch nó theo đúng hướng cạnh đang vuốt (trái/phải), tạo
 *   cảm giác "kéo dần ra để lộ màn trước" giống Android hiện đại.
 * - handleOnBackPressed: được gọi khi người dùng THẢ TAY và cử chỉ hoàn tất -> thực thi hành
 *   động back thật sự ([onBack], ví dụ finish() hoặc quay lại 1 bước trong ứng dụng).
 * - handleOnBackCancelled: được gọi khi người dùng kéo dở rồi rút tay lại giữa chừng (không
 *   hoàn tất cử chỉ) -> animate mượt nội dung trở lại đúng như ban đầu (100% scale, vị trí gốc).
 *
 * Trên các máy/API < 33 (không hỗ trợ predictive back ở tầng hệ thống), các hàm
 * handleOnBackStarted/Progressed/Cancelled đơn giản là không được gọi - hành vi tự động
 * "rớt" về giống như back thường (không animation), không cần code riêng xử lý.
 */
class PredictiveBackHelper(
    private val activity: Activity,
    private val contentView: View,
    // Tỉ lệ scale nhỏ nhất khi kéo hết cỡ (progress = 1f). Vd 0.9f = thu nhỏ còn 90%.
    private val minScale: Float = 0.9f,
    // Khoảng dịch chuyển ngang tối đa (px) khi kéo hết cỡ, tạo cảm giác bị "kéo" theo tay.
    private val maxTranslatePx: Float = 48f,
    // Hành động back thật sự khi cử chỉ hoàn tất (thường là finish(), hoặc logic back riêng
    // của từng màn hình như WebView.goBack(), thoát thư mục con...).
    private val onBack: () -> Unit
) {
    fun register(dispatcher: OnBackPressedDispatcher) {
        dispatcher.addCallback(activity, object : OnBackPressedCallback(true) {

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                contentView.pivotY = contentView.height / 2f
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                val progress = backEvent.progress.coerceIn(0f, 1f)

                val scale = 1f - (1f - minScale) * progress
                contentView.scaleX = scale
                contentView.scaleY = scale

                // EDGE_LEFT (vuốt từ mép trái vào) -> nội dung dịch sang phải theo tay.
                // EDGE_RIGHT (vuốt từ mép phải vào) -> nội dung dịch sang trái theo tay.
                val direction = if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                contentView.translationX = direction * maxTranslatePx * progress
            }

            override fun handleOnBackPressed() {
                onBack()
                // Nếu onBack() không kết thúc Activity (vd chỉ lùi 1 bước trong app, như
                // WebView.goBack() hoặc thoát thư mục con), khôi phục lại giao diện về trạng
                // thái ban đầu vì người dùng vẫn đang ở lại màn hình này.
                // Nếu Activity đang finish() thì bỏ qua reset để animation thu nhỏ tiếp tục
                // mượt mà cho tới khi màn hình biến mất, không bị "giật" về rồi mới đóng.
                if (!activity.isFinishing) {
                    resetContentView(animated = true)
                }
            }

            override fun handleOnBackCancelled() {
                resetContentView(animated = true)
            }

            private fun resetContentView(animated: Boolean) {
                if (animated) {
                    contentView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationX(0f)
                        .setDuration(180)
                        .start()
                } else {
                    contentView.scaleX = 1f
                    contentView.scaleY = 1f
                    contentView.translationX = 0f
                }
            }
        })
    }
}
