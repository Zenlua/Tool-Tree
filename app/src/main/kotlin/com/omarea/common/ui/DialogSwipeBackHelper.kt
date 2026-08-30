package com.omarea.common.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * Vuốt sang phải ở bất kỳ đâu trên nội dung 1 dialog TOÀN MÀN HÌNH để đóng dialog lại - cùng
 * cảm giác/cơ chế với vuốt lùi toàn trang ở ActionPage (xem com.tool.tree.ui.SwipeBackHelper).
 * Chỉ hỗ trợ vuốt trực tiếp trên nội dung dialog - KHÔNG còn hỗ trợ cử chỉ vuốt-từ-mép
 * (predictive-back của hệ thống) nữa.
 *
 * onDragStateChanged(true)/onDragProgress(0f..1f) dành cho bên gọi nếu cần thêm hiệu ứng phụ
 * lúc kéo - DialogFullScreen hiện không dùng gì thêm vì nền cửa sổ dialog đã sẵn là ảnh NÉT của
 * cửa sổ thật phía sau (xem DialogHelper.setWindowBlurBgWithSharpCopy()), tự lộ ra khi
 * contentView trượt đi mà không cần thêm view/animation nào khác.
 *
 * Dùng dispatchTouchEvent() gọi từ TRƯỚC khi phát sự kiện chạm cho cây view con (xem
 * bind()/DialogFullScreen) để có thể "giành" cử chỉ kéo ngang ngay khi phát hiện, đồng thời vẫn
 * để tap/cuộn dọc bình thường đi xuống cho view con khi không phải là cử chỉ vuốt lùi.
 */
class DialogSwipeBackHelper(
    context: Context,
    private val contentView: View,
    private val onDragStateChanged: (dragging: Boolean) -> Unit = {},
    private val onDragProgress: (progress: Float) -> Unit = {},
    private val onBack: () -> Unit
) {
    companion object {
        private const val COMMIT_DISTANCE_RATIO = 0.28f
        private const val SETTLE_DURATION_MIN_MS = 150L
        private const val SETTLE_DURATION_MAX_MS = 300L

        /**
         * Gắn tính năng vuốt lùi cho 1 Dialog TOÀN MÀN HÌNH bất kỳ ĐÃ show (dialog.window khác
         * null) bằng cách bọc lại Window.Callback hiện có - áp dụng được cho cả Dialog dựng qua
         * AlertDialog.Builder lẫn Dialog thường, không cần tạo riêng 1 lớp con Dialog. Trả về
         * helper để bên gọi có thể release() đúng lúc (tránh leak VelocityTracker/animator).
         */
        fun bind(
            dialog: Dialog,
            contentView: View,
            onDragStateChanged: (dragging: Boolean) -> Unit = {},
            onDragProgress: (progress: Float) -> Unit = {},
            onBack: () -> Unit
        ): DialogSwipeBackHelper? {
            val window = dialog.window ?: return null
            val helper = DialogSwipeBackHelper(contentView.context, contentView, onDragStateChanged, onDragProgress, onBack)
            val original = window.callback
            window.callback = object : Window.Callback by original {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                    if (helper.dispatchTouchEvent(event)) return true
                    return original.dispatchTouchEvent(event)
                }
            }
            return helper
        }
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    // Đổ bóng nhẹ ở cạnh trái trong lúc kéo, giống hệt hiệu ứng ở SwipeBackHelper (trang toàn
    // màn hình) - cho cảm giác dialog đang được "nhấc lên" khỏi cửa sổ thật phía sau.
    private val dragElevationPx = 8f * context.resources.displayMetrics.density

    // Giới hạn kéo tối đa khi vuốt SANG TRÁI (nảy rubber-band) - xem SwipeBounceEffect
    private val maxLeftPullPx = com.tool.tree.ui.SwipeBounceEffect.maxPullPx(context.resources.displayMetrics.density)

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var candidate = false
    private var dragging = false

    // Đã xác nhận là đang kéo NẢY sang trái (rubber-band, không dẫn tới hành động gì, chỉ để
    // phản hồi "đã chạm biên") - tách riêng khỏi `dragging` vì 2 hướng có ý nghĩa khác nhau
    private var draggingLeft = false

    private var settleAnimator: ValueAnimator? = null
    var enabled = true

    // Tăng dần mỗi khi 1 phiên kéo MỚI bắt đầu - "đánh dấu" settleAnimator thuộc phiên nào, để
    // 1 animator cũ lỡ chạy xong sau khi phiên mới đã bắt đầu không tự ý gọi
    // onDragStateChanged(false) đè lên trạng thái của phiên đang chạy (race hiếm giữa main
    // thread và animation callback queue).
    private var dragSessionId = 0

    /**
     * Trả về true nghĩa là sự kiện đã bị "giành" bởi cử chỉ vuốt lùi (không cần forward xuống
     * view con nữa). Trả về false thì bên gọi vẫn xử lý sự kiện như bình thường.
     */
    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!enabled) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val isIdle = settleAnimator?.isRunning != true &&
                    contentView.translationX == 0f
                candidate = isIdle
                dragging = false
                draggingLeft = false
                downX = ev.rawX
                downY = ev.rawY
                if (candidate) {
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain()
                    velocityTracker?.addMovement(ev)
                }
                // Luôn để children nhận ACTION_DOWN như bình thường (tap vẫn hoạt động nếu cuối
                // cùng đây không phải là 1 cử chỉ kéo lùi).
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!candidate && !dragging && !draggingLeft) return false
                velocityTracker?.addMovement(ev)
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                if (!dragging && !draggingLeft) {
                    when {
                        dx > touchSlop && dx > abs(dy) -> {
                            beginNewDragSession()
                            dragging = true
                            onDragStateChanged(true)
                            contentView.elevation = dragElevationPx
                            // "Hủy" cử chỉ đang dở dang ở view con (ví dụ đang cuộn list) trước
                            // khi ta bắt đầu tự điều khiển translationX.
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            contentView.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        dx < -touchSlop && abs(dx) > abs(dy) -> {
                            // Vuốt sang trái -> không có hành động nào (đóng dialog chỉ gắn với
                            // vuốt phải), chỉ nảy nhẹ (rubber-band) để phản hồi rồi bật lại
                            beginNewDragSession()
                            draggingLeft = true
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            contentView.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        abs(dy) > touchSlop -> {
                            // Kéo dọc chiếm ưu thế -> không phải cử chỉ ngang, nhường hẳn cho
                            // view con (cuộn list, kéo seekbar...).
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

                if (draggingLeft) {
                    val pulled = com.tool.tree.ui.SwipeBounceEffect.dampen((-dx).coerceAtLeast(0f), maxLeftPullPx)
                    contentView.translationX = -pulled
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                val wasDraggingLeft = draggingLeft
                if (wasDragging) {
                    velocityTracker?.addMovement(ev)
                    velocityTracker?.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                    val velocityX = velocityTracker?.xVelocity ?: 0f
                    settleAfterDrag(velocityX)
                } else if (wasDraggingLeft) {
                    // Luôn bật lại về 0 (không có "commit" nào cho hướng trái) - dùng
                    // interpolator nảy thay vì DecelerateInterpolator thường
                    animateTo(0f, 0f, null, durationMultiplier = 1.3f, bounce = true, notifyStateChange = false)
                }
                recycleTracker()
                candidate = false
                dragging = false
                draggingLeft = false
                return wasDragging || wasDraggingLeft
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
        onDragProgress((dx / width).coerceIn(0f, 1f))
    }

    /** Xem giải thích ở SwipeBackHelper.beginNewDragSession() - dùng lại y hệt cơ chế. */
    private fun beginNewDragSession() {
        settleAnimator?.cancel()
        settleAnimator = null
        dragSessionId++
    }

    private fun settleAfterDrag(velocityX: Float) {
        val width = contentView.width.takeIf { it > 0 } ?: return
        val distance = contentView.translationX
        val shouldGoBack = distance > width * COMMIT_DISTANCE_RATIO || velocityX > minFlingVelocity

        if (shouldGoBack) {
            animateTo(width.toFloat(), velocityX, onEnd = { onBack() })
        } else {
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu.
            animateTo(0f, velocityX, null)
        }
    }

    private fun animateTo(
        target: Float,
        velocityX: Float,
        onEnd: (() -> Unit)?,
        durationMultiplier: Float = 1f,
        bounce: Boolean = false,
        notifyStateChange: Boolean = true
    ) {
        val start = contentView.translationX
        val distance = abs(target - start)
        val duration = (computeSettleDuration(distance, velocityX) * durationMultiplier).toLong()

        // Chụp lại đúng phiên kéo mà animator này thuộc về (xem dragSessionId ở trên).
        val sessionAtStart = dragSessionId

        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            interpolator = if (bounce) com.tool.tree.ui.SwipeBounceEffect.bounceInterpolator else DecelerateInterpolator(1.2f)
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val isStale = dragSessionId != sessionAtStart
                    if (target == 0f && !isStale) {
                        contentView.translationX = 0f
                        contentView.elevation = 0f
                        // notifyStateChange = false cho phiên nảy trái, vì phiên đó chưa từng
                        // gọi onDragStateChanged(true) - không được gọi false đè lên trạng thái
                        if (notifyStateChange) onDragStateChanged(false)
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

    /** Hủy animation & reset trạng thái - gọi lúc dialog bị đóng/hủy để tránh leak. */
    fun release() {
        settleAnimator?.cancel()
        settleAnimator = null
        recycleTracker()
    }
}
