package com.omarea.common.ui

import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * Cho phép vuốt ngang (trái HOẶC phải) [target] để "hủy" nó -- dùng cho banner
 * (BannerNotificationManager). Banner là 1 sub-window riêng add thẳng bằng WindowManager,
 * không nằm trong RecyclerView/ViewPager nào để dùng lại ItemTouchHelper/swipe có sẵn của
 * Android, nên cần tự bắt touch bằng tay.
 *
 * Cách dùng: gắn vào [target] (nên là View "thân" banner, KHÔNG phải các nút bấm bên trong --
 * ViewGroup sẽ tự ưu tiên cho các nút con clickable xử lý touch của riêng chúng trước, xem
 * comment trong [handleTouch]).
 *
 * Vuốt quá [COMMIT_DISTANCE_RATIO] chiều rộng [target] HOẶC đủ nhanh (fling, không cần vuốt xa)
 * theo 1 hướng -> [target] bay tiếp ra khỏi màn hình theo hướng đó rồi gọi [onDismiss]. Thả tay
 * chưa đủ 2 điều kiện trên -> tự bật lại vị trí gốc, KHÔNG gọi [onDismiss].
 */
class BannerSwipeDismissHelper(
    context: Context,
    private val target: View,
    private val onDismiss: () -> Unit
) {
    companion object {
        private const val COMMIT_DISTANCE_RATIO = 0.35f
        private const val SETTLE_DURATION_MS = 200L
        private const val FLY_OUT_DURATION_MS = 180L
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val screenWidthPx = context.resources.displayMetrics.widthPixels

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    // true sau khi đã bay ra khỏi màn hình / gọi onDismiss -- chặn không xử lý touch thêm
    // (vd trường hợp hiếm gặp thêm 1 ACTION_DOWN lọt vào đúng lúc view đang animate biến mất).
    private var dismissed = false

    init {
        target.setOnTouchListener { _, event -> handleTouch(event) }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        if (dismissed) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                // Trả về true ở ACTION_DOWN để target được đăng ký làm touch target với
                // ViewGroup cha -- nếu trả false thì Android sẽ KHÔNG chuyển tiếp
                // MOVE/UP của cùng cử chỉ tới target nữa, khiến không thể vuốt được. Việc
                // này KHÔNG ảnh hưởng tới các nút clickable bên trong (Xác nhận/Hủy bỏ):
                // khi ngón tay đặt đúng lên chúng, ViewGroup luôn ưu tiên cho nút con tự
                // xử lý trước qua vòng dispatch-tới-con, handleTouch() chỉ thực sự được
                // gọi (fallback) khi điểm chạm KHÔNG rơi vào nút nào.
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!dragging) {
                    // Chỉ nhận là đang vuốt ngang khi di chuyển đủ xa (qua touchSlop) VÀ theo
                    // chiều ngang rõ ràng hơn chiều dọc -- tránh nuốt mất thao tác cuộn dọc
                    // (nếu sau này banner có nội dung dài cần cuộn).
                    if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                        dragging = true
                        target.parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        return false
                    }
                }
                target.translationX = dx
                // Mờ dần theo khoảng cách kéo, tối thiểu 30% để vẫn thấy còn đang kéo dở.
                target.alpha = (1f - abs(dx) / target.width.coerceAtLeast(1)).coerceIn(0.3f, 1f)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    return false
                }
                dragging = false
                val tracker = velocityTracker
                tracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                val velocityX = tracker?.xVelocity ?: 0f
                tracker?.recycle()
                velocityTracker = null

                val dx = target.translationX
                val width = target.width.coerceAtLeast(1)
                val shouldDismiss = abs(dx) > width * COMMIT_DISTANCE_RATIO || abs(velocityX) > minFlingVelocity
                if (shouldDismiss) {
                    flyOutAndDismiss(if (dx >= 0f) 1 else -1)
                } else {
                    settleBack()
                }
                return true
            }
        }
        return false
    }

    private fun flyOutAndDismiss(direction: Int) {
        dismissed = true
        // Bay hẳn ra ngoài rìa màn hình (không chỉ hết chiều rộng target) để chắc chắn khuất
        // hoàn toàn trước khi onDismiss() gỡ view, tránh giật hình 1 khung hình cuối.
        val distance = (target.width + screenWidthPx).toFloat()
        target.animate()
            .translationX(direction * distance)
            .alpha(0f)
            .setDuration(FLY_OUT_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onDismiss() }
            .start()
    }

    private fun settleBack() {
        target.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(SETTLE_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
}
