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
 * onDragStateChanged(true) được gọi ngay khi xác nhận là đang kéo (CẢ 2 hướng phải/trái) để
 * phía Activity hiện ảnh preview mờ/nét lên; onDragStateChanged(false) được gọi khi kéo bị
 * hủy/nảy xong và đã bật lại về vị trí gốc (không gọi khi kéo phải THÀNH CÔNG vì activity sắp
 * finish() rồi, không cần ẩn preview làm gì nữa).
 *
 * onDragProgress(0f..1f) được gọi liên tục theo khoảng cách đã kéo (0 = chưa kéo, 1 = kéo
 * hết chiều rộng màn hình) - dùng để hiện hiệu ứng "lấy nét dần" cho ảnh preview phía sau:
 * vuốt càng nhiều thì ảnh preview càng nét/rõ hơn.
 *
 * CHỈ hỗ trợ vuốt tự theo dõi bằng tay qua dispatchTouchEvent() (vuốt ở bất kỳ đâu trên màn
 * hình) - KHÔNG hỗ trợ cử chỉ vuốt-từ-mép predictive-back của hệ thống (Android 13+/14+ gesture
 * nav), tính năng đó đã bị loại bỏ.
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

    // Giới hạn kéo tối đa khi vuốt SANG TRÁI (nảy rubber-band) - xem SwipeBounceEffect
    private val maxLeftPullPx = SwipeBounceEffect.maxPullPx(activity.resources.displayMetrics.density)

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

    // Đã xác nhận là đang kéo NẢY sang trái (rubber-band, không dẫn tới hành động gì, chỉ để
    // phản hồi "đã chạm biên") - tách riêng khỏi `dragging` vì 2 hướng có ý nghĩa khác nhau
    private var draggingLeft = false

    private var settleAnimator: ValueAnimator? = null
    var enabled = true

    // Tăng dần mỗi khi 1 phiên kéo MỚI bắt đầu - dùng để "đánh dấu" settleAnimator được tạo ra
    // thuộc về phiên nào. Nếu 1 animator cũ lỡ chạy xong (onAnimationEnd) SAU KHI 1 phiên kéo
    // mới đã bắt đầu (dù bình thường đã bị cancel() ở đầu phiên mới - xem beginNewDragSession()
    // - nhưng cancel() không tuyệt đối chặn được mọi race hiếm giữa main thread và animation
    // callback queue), nó sẽ tự nhận ra mình đã "lỗi thời" và KHÔNG được phép gọi
    // onDragStateChanged(false) đè lên trạng thái của phiên đang chạy.
    private var dragSessionId = 0

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
                // Luôn để children nhận ACTION_DOWN như bình thường - tap vẫn hoạt động bình
                // thường nếu cuối cùng đây không phải là 1 cử chỉ kéo lùi
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!candidate && !dragging && !draggingLeft) return false
                velocityTracker?.addMovement(ev)

                val dx = ev.rawX - downX
                val dy = ev.rawY - downY

                if (!dragging && !draggingLeft) {
                    when {
                        dx > touchSlop && dx > abs(dy) * 1.2f -> {
                            // Xác nhận là kéo lùi -> hủy sự kiện đang dở dang (nếu có) trên
                            // view con, ví dụ đang scroll dọc hoặc đang nhấn giữ 1 item
                            beginNewDragSession()
                            dragging = true
                            onDragStateChanged(true)
                            contentView.elevation = dragElevationPx
                            // Đã loại bỏ gỡ/đổi background tại đây để giữ nguyên ảnh nền
                            
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            contentView.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        dx < -touchSlop && abs(dx) > abs(dy) * 1.2f -> {
                            // Vuốt sang trái -> không có hành động điều hướng nào, chỉ nảy nhẹ
                            // (rubber-band) để phản hồi rồi bật lại - xem SwipeBounceEffect.
                            // Vẫn hiện preview mờ/nét giống hệt bên phải (onDragStateChanged)
                            // để có cảm giác đồng nhất, dù không thật sự "lùi" về trang nào.
                            beginNewDragSession()
                            draggingLeft = true
                            onDragStateChanged(true)
                            val cancelEvent = MotionEvent.obtain(ev)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            contentView.dispatchTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                        abs(dy) > touchSlop -> {
                            // Kéo dọc chiếm ưu thế -> không phải cử chỉ ngang, nhường hẳn cho
                            // view con (cuộn list, ...)
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
                    val pulled = SwipeBounceEffect.dampen((-dx).coerceAtLeast(0f), maxLeftPullPx)
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
                    // interpolator nảy thay vì DecelerateInterpolator thường. notifyStateChange
                    // = true (đã đổi từ false) vì giờ nhánh vuốt trái CŨNG gọi
                    // onDragStateChanged(true) lúc bắt đầu (để hiện preview mờ giống bên phải)
                    // nên phải tắt lại đúng lúc kết thúc, tránh preview bị treo mãi.
                    animateTo(0f, 0f, null, durationMultiplier = 1.3f, bounce = true, notifyStateChange = true)
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
        // Chỉ dịch chuyển vị trí, KHÔNG làm mờ/trong suốt nội dung đang kéo
        contentView.translationX = dx
        val width = contentView.width.takeIf { it > 0 } ?: 1
        onDragProgress((dx / width).coerceIn(0f, 1f))
    }

    /**
     * Gọi ngay khi 1 phiên kéo MỚI được xác nhận, TRƯỚC khi set dragging = true. Cancel ngay
     * settleAnimator của phiên trước (nếu còn đang bounce dở) để tránh 2 nguồn cùng ghi
     * contentView.translationX song song, đồng thời tăng dragSessionId để "khóa" animator cũ
     * lại - dù cancel() không kịp chặn onAnimationEnd của nó (race hiếm), animator cũ vẫn tự
     * nhận ra mình lỗi thời qua token và không gọi onDragStateChanged(false) đè lên phiên mới
     * đang chạy.
     */
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
            animateTo(width.toFloat(), velocityX, { onBack() })
        } else {
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu, có nảy nhẹ
            // (bounce) giống hiệu ứng vuốt trái, thay vì trượt về đều đều như trước
            animateTo(0f, velocityX, null, durationMultiplier = 1.3f, bounce = true)
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

        // Chụp lại đúng phiên kéo mà animator này thuộc về - nếu lúc animator chạy xong mà
        // dragSessionId đã đổi khác (nghĩa là 1 phiên kéo mới đã bắt đầu đè lên, xem
        // beginNewDragSession()), animator này coi như "lỗi thời" và không được phép tự ý tắt
        // preview (onDragStateChanged(false)) của phiên mới.
        val sessionAtStart = dragSessionId

        settleAnimator?.cancel()
        settleAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            interpolator = if (bounce) SwipeBounceEffect.bounceInterpolator else DecelerateInterpolator(1.2f)
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val isStale = dragSessionId != sessionAtStart
                    if (target == 0f && !isStale) {
                        // Đảm bảo về đúng trạng thái gốc - chỉ khi animator này vẫn còn thuộc
                        // về phiên kéo hiện tại (không bị 1 phiên mới đè lên giữa chừng)
                        contentView.translationX = 0f
                        contentView.elevation = 0f
                        // Đã loại bỏ gán background = null tại đây
                        // notifyStateChange mặc định = true, tắt preview đúng lúc animation về 0
                        // kết thúc - áp dụng cho CẢ 2 hướng (phải: hủy kéo giữa chừng; trái: nảy
                        // rubber-band), vì giờ cả 2 đều gọi onDragStateChanged(true) lúc bắt đầu.
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

    /** Hủy animation & reset trạng thái - gọi ở onDestroy() của Activity để tránh leak. */
    fun release() {
        settleAnimator?.cancel()
        settleAnimator = null
        recycleTracker()
    }
}