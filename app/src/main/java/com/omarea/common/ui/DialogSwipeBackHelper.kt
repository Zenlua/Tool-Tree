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
 * cảm giác/cơ chế với vuốt lùi toàn trang ở ActionPage (xem com.tool.tree.ui.SwipeBackHelper),
 * kể cả hỗ trợ cử chỉ vuốt-từ-mép (predictive-back của hệ thống, Android 13+/14+) qua
 * onSystemBackStarted()/onSystemBackProgress()/onSystemBackCancelled()/consumeSystemBackInvoked()
 * - bên gọi (DialogFullScreen) tự đăng ký các callback này với
 * dialog.window.onBackInvokedDispatcher, vì Dialog có Window RIÊNG, không tự động dùng chung
 * onBackPressedDispatcher của Activity.
 *
 * onDragStateChanged(true)/onDragProgress(0f..1f) dùng để bên gọi hiện/crossfade 1 ảnh chụp
 * (nét/mờ) của cửa sổ thật phía sau ngay trong lúc kéo - xem DialogFullScreen (dùng
 * DialogHelper.setWindowBlurBgWithSharpCopy() để có bản nét).
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
         * helper để bên gọi có thể release() đúng lúc (tránh leak VelocityTracker/animator), và
         * để đăng ký thêm predictive-back (xem onSystemBackStarted()...).
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

    private var velocityTracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var candidate = false
    private var dragging = false
    private var settleAnimator: ValueAnimator? = null
    var enabled = true

    // true khi đang có 1 cử chỉ predictive-back của hệ thống (vuốt từ mép) điều khiển tiến độ,
    // thay vì do dispatchTouchEvent() theo dõi trực tiếp ngón tay - xem SwipeBackHelper (bản
    // toàn trang) để hiểu rõ cơ chế chung, ở đây dùng lại y hệt.
    private var externalDragActive = false

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
                    contentView.translationX == 0f &&
                    !externalDragActive
                candidate = isIdle
                dragging = false
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
                if (!candidate && !dragging) return false
                velocityTracker?.addMovement(ev)
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                if (!dragging) {
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
                        dx < -touchSlop || abs(dy) > touchSlop -> {
                            // Kéo sang trái hoặc kéo dọc -> không phải cử chỉ đóng dialog, nhường
                            // hẳn cho view con (cuộn list, kéo seekbar...).
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
            animateTo(width.toFloat(), velocityX) { onBack() }
        } else {
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu.
            animateTo(0f, velocityX, null)
        }
    }

    private fun animateTo(target: Float, velocityX: Float, onEnd: (() -> Unit)?) {
        val start = contentView.translationX
        val distance = abs(target - start)
        val duration = computeSettleDuration(distance, velocityX)

        // Chụp lại đúng phiên kéo mà animator này thuộc về (xem dragSessionId ở trên).
        val sessionAtStart = dragSessionId

        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val isStale = dragSessionId != sessionAtStart
                    if (target == 0f && !isStale) {
                        contentView.translationX = 0f
                        contentView.elevation = 0f
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

    /** Hủy animation & reset trạng thái - gọi lúc dialog bị đóng/hủy để tránh leak. */
    fun release() {
        settleAnimator?.cancel()
        settleAnimator = null
        recycleTracker()
    }

    // ================== Predictive-back (Android 13+, vuốt từ mép) ==================
    // Bên gọi (DialogFullScreen) tự đăng ký các hàm này với dialog.window.onBackInvokedDispatcher
    // (Dialog có Window riêng, KHÔNG dùng chung onBackPressedDispatcher của Activity như trang
    // toàn màn hình - xem SwipeBackHelper). Logic bên trong giống hệt SwipeBackHelper để 2 nơi
    // cho cảm giác nhất quán.

    /** Gọi khi hệ thống vừa xác nhận người dùng bắt đầu vuốt từ mép để đóng dialog. */
    fun onSystemBackStarted() {
        if (dragging) return
        beginNewDragSession()
        dragging = true
        externalDragActive = true
        onDragStateChanged(true)
        contentView.elevation = dragElevationPx
    }

    /** Gọi liên tục theo tiến độ vuốt từ mép (0f..1f). */
    fun onSystemBackProgress(progress: Float) {
        if (!externalDragActive) return
        val width = contentView.width.takeIf { it > 0 } ?: return
        val dampedProgress = progress.coerceIn(0f, 1f) / 3.75f
        applyProgress(dampedProgress * width)
    }

    /** Gọi khi người dùng buông tay/hủy giữa chừng cử chỉ vuốt từ mép. */
    fun onSystemBackCancelled() {
        if (!externalDragActive) return
        externalDragActive = false
        dragging = false
        animateTo(0f, 0f, null)
    }

    /**
     * Gọi khi hệ thống xác nhận cử chỉ vuốt từ mép đã hoàn tất (tương đương bấm back). Trả về
     * true nếu đã tự xử lý (đang có cử chỉ dở dang) - bên gọi KHÔNG cần tự dismiss() thêm. Trả
     * về false nghĩa là không có gì để xử lý, bên gọi tự quyết định (thường là dismiss() ngay).
     */
    fun consumeSystemBackInvoked(): Boolean {
        if (!externalDragActive) return false
        externalDragActive = false
        dragging = false
        val width = contentView.width.takeIf { it > 0 }?.toFloat() ?: return false
        animateTo(width, 0f) { onBack() }
        return true
    }
}
