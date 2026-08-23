package com.tool.tree.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Outline
import android.os.Build
import android.view.MotionEvent
import android.view.RoundedCorner
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
 *
 * dragBackgroundColor: màu nền sẽ tạm thời gán cho contentView TRONG LÚC kéo (và gỡ ra ngay
 * khi kéo bị hủy/bật lại) - contentView vốn không có background riêng để không che nền/blur
 * hình nền lúc đứng yên, nhưng vì vậy các khoảng trống bên trong nó (viền list, khoảng cách
 * item...) sẽ hở ra ảnh preview phía sau ngay cả ở phần "chưa kéo tới", trông như bị trong
 * suốt. Gán tạm 1 màu nền đặc trong lúc kéo sẽ khắc phục việc này mà không ảnh hưởng gì lúc
 * đứng yên.
 */
class SwipeBackHelper(
    private val activity: Activity,
    private val contentView: View,
    private val onDragStateChanged: (dragging: Boolean) -> Unit = {},
    private val onDragProgress: (progress: Float) -> Unit = {},
    private val dragBackgroundColor: Int? = null,
    private val onBack: () -> Unit = { activity.finish(); activity.overridePendingTransition(0, 0) }
) {
    companion object {
        // Kéo quá tỉ lệ này của chiều rộng màn hình (dù thả tay chậm) -> coi như xác nhận trở lại
        private const val COMMIT_DISTANCE_RATIO = 0.28f

        private const val SETTLE_DURATION_MIN_MS = 150L
        private const val SETTLE_DURATION_MAX_MS = 300L

        // Cử chỉ predictive-back của hệ thống (gesture-nav vuốt từ mép, Android 13+): lúc còn
        // đang GIỮ TAY kéo thì cố ý cho trôi chậm lại 1 nửa tốc độ so với progress hệ thống báo
        // (xem onSystemBackProgress) - còn lúc THẢ TAY ra (dù bật lại hay trượt nốt để trở lại)
        // thì animation chạy tốc độ bình thường (1:1, giống hệt lúc vuốt tay trên toàn màn
        // hình), không nhân đôi thời lượng nữa.
        private const val SYSTEM_BACK_PROGRESS_DAMPING = 2f
    }

    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(activity).scaledMinimumFlingVelocity
    private val maxFlingVelocity = ViewConfiguration.get(activity).scaledMaximumFlingVelocity

    // Đổ bóng nhẹ ở cạnh trái trong lúc kéo, cho cảm giác giống 1 "cửa sổ" đang được nhấc lên
    // và kéo trượt sang phải (thay vì chỉ là 1 lớp phẳng lì di chuyển)
    private val dragElevationPx = 8f * activity.resources.displayMetrics.density

    init {
        contentView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                var applied = false

                // Chỉ lấy bán kính bo góc thật phần cứng nếu thiết bị chạy Android 12 (API 31)+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val insets = view.rootWindowInsets
                    val radius = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius
                    if (radius != null && radius > 0) {
                        outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
                        applied = true
                    }
                }

                // Android thấp hơn (API < 31) hoặc không lấy được radius -> giữ khung vuông
                if (!applied) {
                    outline.setRect(0, 0, view.width, view.height)
                }
                outline.alpha = 1f
            }
        }

        // Chỉ bật cắt viền nội dung (clipToOutline) đối với Android 12 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contentView.clipToOutline = true
        }
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
                            if (dragBackgroundColor != null) {
                                contentView.setBackgroundColor(dragBackgroundColor)
                            }
                            // Bật hardware layer trong lúc kéo: cả subtree (toolbar blur +
                            // list + fab) được "chụp" lại thành 1 texture GPU rồi chỉ dịch
                            // chuyển texture đó mỗi khung hình, thay vì phải vẽ lại toàn bộ
                            // cây view (kèm shadow do elevation) ở mỗi frame - đây là nguyên
                            // nhân chính gây nhấp nháy ở các khoảng trống khi kéo nhanh trên
                            // một số thiết bị/driver GPU.
                            contentView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
        // Chỉ dịch chuyển vị trí, KHÔNG làm mờ/trong suốt nội dung đang kéo - để trang hiện
        // tại trông như 1 "cửa sổ" đặc, kéo trượt sang phải, để lộ ảnh preview phía sau,
        // thay vì cảm giác bị mờ/xuyên thấu khi chồng lên ảnh preview.
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
            // Kéo chưa đủ hoặc vuốt ngược lại -> bật lại về vị trí ban đầu, tốc độ "settle"
            // co giãn theo khoảng cách còn lại + vận tốc thả tay, giống cách SwipePager làm
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
                        // Đảm bảo về đúng trạng thái gốc, tránh sai số cộng dồn của animator
                        contentView.translationX = 0f
                        contentView.elevation = 0f
                        if (dragBackgroundColor != null) {
                            // Gỡ màu nền tạm ra để không che nền/blur hình nền lúc đứng yên
                            contentView.background = null
                        }
                        onDragStateChanged(false)
                    }
                    // Tắt hardware layer khi đã kết thúc kéo (dù bật lại hay trượt hẳn ra) -
                    // giữ layer lâu dài không cần thiết sẽ tốn thêm bộ nhớ GPU
                    contentView.setLayerType(View.LAYER_TYPE_NONE, null)
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
    // Các hàm dưới đây được gọi từ OnBackPressedCallback.handleOnBack*() khi hệ điều hành nhận
    // diện cử chỉ vuốt từ mép màn hình cho gesture-nav (predictive back). Cố ý dùng lại đúng
    // applyProgress()/animateTo() với cùng field "dragging" như khi kéo bằng tay thông thường,
    // để chỉ có 1 bộ hiệu ứng nhất quán (dịch chuyển, đổ bóng, màu nền tạm, preview mờ/nét,
    // hardware layer...) cho mọi cách vuốt lùi, không phân biệt nguồn.

    /** Gọi từ handleOnBackStarted() - hệ thống vừa xác nhận người dùng bắt đầu vuốt từ mép. */
    fun onSystemBackStarted() {
        if (dragging) return // đang có 1 cử chỉ khác dở dang (hiếm khi xảy ra) -> bỏ qua
        dragging = true
        externalDragActive = true
        onDragStateChanged(true)
        contentView.elevation = dragElevationPx
        if (dragBackgroundColor != null) {
            contentView.setBackgroundColor(dragBackgroundColor)
        }
        contentView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    /** Gọi từ handleOnBackProgressed() với progress 0f..1f do hệ thống tính sẵn theo khoảng
     *  cách đã vuốt (đã được OS áp interpolator riêng). Cố ý "ghìm" lại còn 1 nửa quãng đường
     *  (chia cho SYSTEM_BACK_PROGRESS_DAMPING) trong lúc còn đang giữ tay kéo - để cửa sổ trôi
     *  chậm gấp đôi so với tốc độ vuốt thật. Lúc buông tay ra (xem onSystemBackCancelled/
     *  consumeSystemBackInvoked) thì KHÔNG áp hệ số này nữa, animation phần còn lại chạy tốc
     *  độ bình thường (1:1) như vuốt tay trên toàn màn hình. */
    fun onSystemBackProgress(progress: Float) {
        if (!externalDragActive) return
        val width = contentView.width.takeIf { it > 0 } ?: return
        val dampedProgress = progress.coerceIn(0f, 1f) / SYSTEM_BACK_PROGRESS_DAMPING
        applyProgress(dampedProgress * width)
    }

    /** Gọi từ handleOnBackCancelled() - người dùng vuốt chưa đủ/thả tay giữa chừng, hệ thống
     *  tự quyết định hủy cử chỉ (không cần tự tính threshold như lúc kéo bằng tay). Đã thả tay
     *  nên chạy animation tốc độ bình thường, không nhân đôi thời lượng nữa. */
    fun onSystemBackCancelled() {
        if (!externalDragActive) return
        externalDragActive = false
        dragging = false
        animateTo(0f, 0f, null)
    }

    /**
     * Gọi từ handleOnBackPressed() - đây là điểm "chốt" cuối cùng cho cả 2 trường hợp: (a) 1
     * cử chỉ predictive-back đã có progress từ trước (Android 13+, gesture-nav), lúc này hàm
     * tự lo animation trượt nốt ra (tốc độ bình thường, vì đã thả tay) rồi finish(), trả về
     * true; (b) back thông thường không có progress (bấm phím back cứng/nav 3 nút, hoặc thiết
     * bị/API cũ không hỗ trợ progress) thì không có gì để "trượt nốt" cả, trả về false để
     * Activity tự finish() bình thường (dùng animation activity_close_enter/exit mặc định
     * theo theme thay vì bị tắt animation).
     */
    fun consumeSystemBackInvoked(): Boolean {
        if (!externalDragActive) return false
        externalDragActive = false
        dragging = false
        val width = contentView.width.takeIf { it > 0 }?.toFloat() ?: return false
        animateTo(width, 0f, { onBack() })
        return true
    }
}
