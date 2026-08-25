package com.tool.tree.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Outline
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
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
 *
 * onDragProgress(0f..1f) được gọi liên tục theo khoảng cách đã kéo (0 = chưa kéo, 1 = kéo
 * hết chiều rộng màn hình) - dùng để hiện hiệu ứng "lấy nét dần" cho ảnh preview phía sau:
 * vuốt càng nhiều thì ảnh preview càng nét/rõ hơn.
 */
class SwipeBackHelper(
    private val activity: Activity,
    private val contentView: View,
    private val onDragStateChanged: (dragging: Boolean) -> Unit = {},
    private val onDragProgress: (progress: Float) -> Unit = {},
    private val onBack: () -> Unit = { activity.finish(); activity.overridePendingTransition(0, 0) }
) {
    companion object {
        // Kéo quá tỉ lệ này của chiều rộng màn hình (dù thả tay chậm) -> coi như xác nhận trở lại
        private const val COMMIT_DISTANCE_RATIO = 0.28f

        private const val SETTLE_DURATION_MIN_MS = 150L
        private const val SETTLE_DURATION_MAX_MS = 300L
    }

    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(activity).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(activity).scaledMaximumFlingVelocity

    // Đổ bóng nhẹ ở cạnh trái trong lúc kéo, cho cảm giác giống 1 "cửa sổ" đang được nhấc lên
    // và kéo trượt sang phải (thay vì chỉ là 1 lớp phẳng lì di chuyển)
    private val dragElevationPx = 8f * activity.resources.displayMetrics.density

    init {
        // contentView (khối bọc toolbar + list) thường KHÔNG tự vẽ background riêng, nó
        // "ăn theo" windowBackground của Activity phía dưới. Bình thường không sao vì
        // contentView phủ kín toàn màn hình - nhưng trong lúc kéo lùi, phía dưới window lại
        // để lộ 1 lớp preview (ảnh màn hình trước) nên nếu contentView không có nền thật thì
        // các khoảng trống giữa toolbar/list sẽ bị "xuyên thấu" ra layer preview đó thay vì
        // giữ đúng màu nền theme hiện tại. Chủ động gán nền thật (chỉ khi contentView chưa có
        // background riêng, để không đè lên nơi khác đã cố ý set) - ưu tiên colorBackground
        // (đúng theo Material theme, cả light/dark), fallback về android:windowBackground nếu
        // theme không khai báo colorBackground.
        applyThemeBackgroundIfMissing()

        contentView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(0, 0, view.width, view.height)
                outline.alpha = 1f
            }
        }
    }

    /**
     * Gán cho contentView ĐÚNG background thật đang hiển thị của Window, nếu nó chưa có
     * background riêng - đọc thẳng activity.window.decorView.background thay vì tự resolve
     * theme attribute (android:windowBackground/colorBackground).
     *
     * Lý do không resolve theme attribute: ThemeModeState có các chế độ "dùng ảnh nền" (level
     * 3-5, xem applyWallpaperMode()) mà ở đó android:windowBackground trong style
     * (AppThemeWallpaper/AppThemeWallpaperLight) bị khai báo = @android:color/transparent một
     * cách CỐ Ý, còn ảnh nền/màu thật thì được gán thẳng vào window sau đó bằng
     * window.setBackgroundDrawable() (ảnh wallpaper tĩnh/custom, hoặc màu window_bg_light/dark
     * khi bật directBg) - nếu chỉ resolve theme attribute sẽ đọc nhầm ra colorBackground mặc
     * định kế thừa từ AppCompat (trắng/đen), làm mất hẳn ảnh nền ngay cả lúc không kéo.
     * decorView.background luôn phản ánh đúng cái đang thật sự hiển thị dù đến từ nguồn nào.
     *
     * Riêng trường hợp Live Wallpaper: ThemeModeState chủ động set window background = null +
     * bật FLAG_SHOW_WALLPAPER để hệ thống tự vẽ hình nền động phía sau - decorBackground sẽ là
     * null, lúc đó CỐ Ý không gán gì (giữ contentView trong suốt), vì bản thân window cũng
     * đang trong suốt để lộ live wallpaper, không có ảnh tĩnh nào để sao chép lại cả.
     *
     * Dùng bản sao Drawable (constantState.newDrawable().mutate()) thay vì gán thẳng chung 1
     * instance với decorView, tránh 2 View cùng chỉnh bounds lên 1 Drawable gây xung đột vẽ.
     */
    private fun applyThemeBackgroundIfMissing() {
        if (contentView.background != null) return
        val decorBackground = activity.window.decorView.background ?: return
        contentView.background = decorBackground.constantState
            ?.newDrawable(activity.resources)
            ?.mutate()
            ?: decorBackground
    }

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

    // true khi đang có 1 cử chỉ predictive-back của hệ thống (Android 13+, vuốt từ mép được
    // OS/gesture-nav nhận trước) điều khiển tiến độ, thay vì do dispatchTouchEvent() ở trên
    // theo dõi trực tiếp ngón tay. Dùng chung applyProgress()/animateTo() với đường vuốt tay
    // thường để có đúng 1 bộ hiệu ứng (dịch chuyển, đổ bóng, preview mờ/nét...) cho cả 2
    // trường hợp.
    private var externalDragActive = false

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
                // dở dang settle từ lần kéo trước, và không phải đang có 1 cử chỉ
                // predictive-back của hệ thống điều khiển)
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
                            contentView.elevation = dragElevationPx
                            // Đã loại bỏ gỡ/đổi background tại đây để giữ nguyên ảnh nền
                            
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
        // Chỉ dịch chuyển vị trí, KHÔNG làm mờ/trong suốt nội dung đang kéo
        contentView.translationX = dx
        val width = contentView.width.takeIf { it > 0 } ?: 1
        onDragProgress((dx / width).coerceIn(0f, 1f))
    }

    private fun settleAfterDrag(velocityX: Float) {
        val width = contentView.width.takeIf { it > 0 } ?: return
        val distance = contentView.translationX
        val shouldGoBack = distance > width * COMMIT_DISTANCE_RATIO || velocityX > minFlingVelocity

        if (shouldGoBack) {
            animateTo(width.toFloat(), velocityX, { onBack() })
        } else {
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu
            animateTo(0f, velocityX, null)
        }
    }

    private fun animateTo(target: Float, velocityX: Float, onEnd: (() -> Unit)?, durationMultiplier: Float = 1f) {
        val start = contentView.translationX
        val distance = abs(target - start)
        val duration = (computeSettleDuration(distance, velocityX) * durationMultiplier).toLong()

        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (target == 0f) {
                        // Đảm bảo về đúng trạng thái gốc
                        contentView.translationX = 0f
                        contentView.elevation = 0f
                        // Đã loại bỏ gán background = null tại đây
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

    // ================== Predictive-back (Android 13+, OnBackPressedCallback) ==================

    /** Gọi từ handleOnBackStarted() - hệ thống vừa xác nhận người dùng bắt đầu vuốt từ mép. */
    fun onSystemBackStarted() {
        if (dragging) return
        dragging = true
        externalDragActive = true
        onDragStateChanged(true)
        contentView.elevation = dragElevationPx
        // Đã loại bỏ gỡ/đổi background tại đây để giữ nguyên ảnh nền
    }

    /** Gọi từ handleOnProgressed() */
    fun onSystemBackProgress(progress: Float) {
        if (!externalDragActive) return
        val width = contentView.width.takeIf { it > 0 } ?: return
        val clampedProgress = progress.coerceIn(0f, 1f)
        val dampedProgress = Math.pow(clampedProgress.toDouble(), 1.5).toFloat()
        applyProgress(dampedProgress * width)
    }

    /** Gọi từ handleOnBackCancelled() */
    fun onSystemBackCancelled() {
        if (!externalDragActive) return
        externalDragActive = false
        dragging = false
        animateTo(0f, 0f, null)
    }

    /** Gọi từ handleOnBackPressed() */
    fun consumeSystemBackInvoked(): Boolean {
        if (!externalDragActive) return false
        externalDragActive = false
        dragging = false
        val width = contentView.width.takeIf { it > 0 }?.toFloat() ?: return false
        animateTo(width, 0f, { onBack() })
        return true
    }
}