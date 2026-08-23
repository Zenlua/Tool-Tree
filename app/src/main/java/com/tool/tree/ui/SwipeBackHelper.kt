package com.tool.tree.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * Vuốt ở bất kỳ đâu trên màn hình (kéo sang phải) để trở lại trang trước - giống hệt hành
 * vi bấm nút back trên toolbar (finish()), nhưng có hiệu ứng kéo theo ngón tay theo thời
 * gian thực thay vì chỉ chạy animation cố định sau khi buông tay.
 *
 * Cách dùng: gọi dispatchTouchEvent() từ Activity.dispatchTouchEvent() TRƯỚC khi gọi
 * super, để có thể "chặn" sự kiện chạm ngay khi phát hiện đang kéo lùi, đồng thời vẫn
 * để các thao tác chạm bình thường (tap, cuộn dọc list...) đi xuống cho các view con xử
 * lý như cũ khi không phải là cử chỉ vuốt lùi.
 *
 * contentView nên là view bọc toàn bộ nội dung (toolbar + list + fab) để cả trang trượt
 * cùng nhau, giống với animation activity_open_enter/activity_close_exit đang dùng khi
 * mở/đóng ActionPage bằng cách thông thường - NHƯNG không nên là root ngoài cùng, vì cần
 * còn 1 lớp phía dưới (ví dụ ảnh preview màn hình cũ) đứng yên để lộ ra trong lúc kéo.
 *
 * onDragStateChanged(true) được gọi ngay khi xác nhận là đang kéo lùi (để phía Activity
 * hiện ảnh preview lên); onDragStateChanged(false) được gọi khi kéo bị hủy và đã bật lại
 * về vị trí gốc (không gọi khi kéo thành công vì activity sắp finish() rồi, không cần ẩn
 * preview làm gì nữa).
 */
class SwipeBackHelper(
    private val activity: Activity,
    private val contentView: View,
    private val onDragStateChanged: (dragging: Boolean) -> Unit = {},
    private val onBack: () -> Unit = { activity.finish(); activity.overridePendingTransition(0, 0) }
) {
    companion object {
        // Kéo quá tỉ lệ này của chiều rộng màn hình (dù thả tay chậm) -> coi như xác nhận trở lại
        private const val COMMIT_DISTANCE_RATIO = 0.28f

        // Độ mờ tối đa khi kéo hết cỡ (mô phỏng lại alpha 0.85 dùng trong activity_close_enter/
        // activity_open_exit, để cảm giác nhất quán với animation chuyển trang có sẵn)
        private const val MAX_ALPHA_FADE = 0.12f

        private const val SETTLE_DURATION_MIN_MS = 150L
        private const val SETTLE_DURATION_MAX_MS = 300L
    }

    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(activity).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(activity).scaledMaximumFlingVelocity

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f

    // Chạm bắt đầu khi trang đang ở trạng thái yên (translationX = 0), đang chờ xem có
    // phải là kéo ngang sang phải hay không
    private var candidate = false

    // Đã xác nhận là đang kéo lùi (đã vượt touchSlop theo chiều ngang)
    private var dragging = false

    private var settleAnimator: ValueAnimator? = null
    var enabled = true

    /**
     * Gọi từ Activity.dispatchTouchEvent(). Trả về true nghĩa là sự kiện đã được xử lý bởi
     * cử chỉ vuốt lùi (không cần forward xuống view con nữa). Trả về false thì Activity vẫn
     * gọi super.dispatchTouchEvent(ev) như bình thường.
     */
    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!enabled) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Chỉ bắt đầu 1 cử chỉ mới khi trang đang ở vị trí gốc (không phải đang
                // dở dang settle từ lần kéo trước)
                val isIdle = settleAnimator?.isRunning != true && contentView.translationX == 0f
                candidate = isIdle
                dragging = false
                downX = ev.rawX
                downY = ev.rawY
                if (candidate) {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(ev)
                }
                // Luôn để children nhận ACTION_DOWN như bình thường - tap vẫn hoạt động bình
                // thường nếu cuối cùng đây không phải là 1 cử chỉ kéo lùi
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!candidate && !dragging) return false
                velocityTracker?.addMovement(ev)

                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                if (!dragging) {
                    when {
                        dx > touchSlop && dx > abs(dy) * 1.2f -> {
                            // Xác nhận là kéo lùi -> hủy sự kiện đang dở dang (nếu có) trên
                            // view con, ví dụ đang scroll dọc hoặc đang nhấn giữ 1 item
                            dragging = true
                            onDragStateChanged(true)
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            contentView.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        dx < -touchSlop || abs(dy) > touchSlop -> {
                            // Kéo sang trái hoặc kéo dọc -> không phải cử chỉ trở lại, nhường
                            // hẳn cho view con (cuộn list, ...)
                            candidate = false
                            return false
                        }
                        else -> return false
                    }
                }

                if (dragging) {
                    val clampedDx = dx.coerceIn(0f, contentView.width.toFloat().coerceAtLeast(1f))
                    applyProgress(clampedDx)
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                if (wasDragging) {
                    velocityTracker?.addMovement(ev)
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    settleAfterDrag(velocityX)
                }
                recycleTracker()
                candidate = false
                dragging = false
                return wasDragging
            }
        }
        return false
    }

    private fun recycleTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun applyProgress(dx: Float) {
        contentView.translationX = dx
        val width = contentView.width.takeIf { it > 0 } ?: 1
        val fraction = (dx / width).coerceIn(0f, 1f)
        contentView.alpha = 1f - MAX_ALPHA_FADE * fraction
    }

    private fun settleAfterDrag(velocityX: Float) {
        val width = contentView.width.takeIf { it > 0 } ?: return
        val distance = contentView.translationX
        val shouldGoBack = distance > width * COMMIT_DISTANCE_RATIO || velocityX > minFlingVelocity

        if (shouldGoBack) {
            animateTo(width.toFloat(), velocityX) { onBack() }
        } else {
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu, tốc độ "settle"
            // co giãn theo khoảng cách còn lại + vận tốc thả tay, giống cách SwipePager làm
            animateTo(0f, velocityX, null)
        }
    }

    private fun animateTo(target: Float, velocityX: Float, onEnd: (() -> Unit)?) {
        val start = contentView.translationX
        val distance = abs(target - start)
        val duration = computeSettleDuration(distance, velocityX)

        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (target == 0f) {
                        // Đảm bảo về đúng trạng thái gốc, tránh sai số cộng dồn của animator
                        contentView.translationX = 0f
                        contentView.alpha = 1f
                        onDragStateChanged(false)
                    }
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    private fun computeSettleDuration(distancePx: Float, velocityPxPerSec: Float): Long {
        if (distancePx <= 0f) return SETTLE_DURATION_MIN_MS
        val velocity = abs(velocityPxPerSec).coerceAtLeast(1f)
        val estimatedMs = (distancePx / velocity * 1000).toLong()
        return estimatedMs.coerceIn(SETTLE_DURATION_MIN_MS, SETTLE_DURATION_MAX_MS)
    }

    /** Hủy animation & reset trạng thái - gọi ở onDestroy() của Activity để tránh leak. */
    fun release() {
        settleAnimator?.cancel()
        settleAnimator = null
        recycleTracker()
    }
}
