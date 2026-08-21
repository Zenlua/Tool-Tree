package com.tool.tree

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/**
 * Tự dựng cử chỉ vuốt mép trái để "trở lại" bằng touch event thô, KHÔNG dùng
 * OnBackPressedCallback.handleOnBackProgressed của hệ thống (androidx.activity) - vì API đó
 * chỉ được Android gửi tiến độ vuốt thật (BackEventCompat.progress) khi app target SDK 33+
 * (predictive back), trong khi app này bắt buộc giữ targetSdkVersion 28 (ứng dụng cần quyền
 * root/shell, không thể nâng target SDK) nên sẽ không bao giờ nhận được sự kiện đó - back luôn
 * bị hệ thống coi là "tức thời", không có tiến độ.
 *
 * QUAN TRỌNG - vì sao KHÔNG dùng View.setOnTouchListener(root) như bản đầu tiên: hầu hết mọi
 * "kênh" đều hiện danh sách mục qua RecyclerView phủ full-width sát tận mép trái (xem
 * activity_action_page.xml, FrameLayout main_list không có margin trái). Với 1 ViewGroup, nếu
 * có bất kỳ view con nào (item trong danh sách) tiêu thụ ACTION_DOWN, toàn bộ chuỗi sự kiện
 * MOVE/UP tiếp theo sẽ đi thẳng vào view con đó - onTouchListener gắn trên view cha (root)
 * KHÔNG BAO GIỜ được gọi nữa dù có canh đúng vùng mép hay không. Đây chính là lý do bản trước
 * "vào một kênh rồi vuốt trở lại" không thấy hiệu ứng: mục danh sách ở ngay mép trái đã tự
 * giữ mất sự kiện trước khi tới lượt root.
 *
 * Cách khắc phục: phải xử lý ở tầng Activity.dispatchTouchEvent() - nơi nhận MỌI sự kiện chạm
 * ĐẦU TIÊN, trước cả khi nó được phát xuống cây view. Gọi [dispatchTouchEvent] từ
 * Activity.dispatchTouchEvent() (xem ActionPage.kt) TRƯỚC khi gọi super:
 * - ACTION_DOWN trong vùng mép: vẫn cho đi xuống bình thường (return false) để không phá vỡ
 *   hiệu ứng nhấn/ripple của item khi người dùng chỉ chạm/tap thường, chỉ đứng ra "làm chứng".
 * - ACTION_MOVE: một khi xác định đúng là vuốt ngang từ mép (vượt touchSlop, dx > dy) thì gửi
 *   một ACTION_CANCEL giả xuống cây view (huỷ trạng thái pressed / cuộn dở của RecyclerView),
 *   sau đó TỰ xử lý toàn bộ MOVE/UP còn lại, không forward cho Activity nữa.
 * - Nếu không phải hướng vuốt back (vuốt dọc để cuộn danh sách, hoặc chạm ngoài vùng mép) thì
 *   không can thiệp gì, mọi thứ hoạt động như chưa từng có helper này.
 *
 * Hiệu ứng dịch chuyển giống hệt res/anim/activity_close_exit.xml (translateX 0%p -> 100%p,
 * alpha giữ nguyên) nhưng bám theo % vuốt thực tế thay vì chạy cố định 350ms.
 *
 * CÒN 1 TẦNG CHẶN NỮA Ở TRÊN CẢ Activity.dispatchTouchEvent(): trên Android 10+ dùng điều
 * hướng cử chỉ (gesture navigation - chế độ phổ biến nhất hiện nay), HỆ THỐNG sẽ tự nuốt luôn
 * sự kiện chạm bắt đầu ở dải mép trái/phải màn hình để làm cử chỉ back CỦA HỆ THỐNG, TRƯỚC KHI
 * touch đó được gửi tới app - dù app có override dispatchTouchEvent đúng cách đến đâu cũng
 * không nhận được sự kiện, vì nó bị chặn ở tầng WindowManager/SystemUI, không phải ở tầng
 * view của app. Đây là lý do dù đã chuyển sang xử lý ở dispatchTouchEvent vẫn không thấy hiệu
 * ứng. Phải gọi [installGestureExclusion] để khai báo với hệ thống "vùng mép này app tự xử lý,
 * đừng cướp" bằng View.setSystemGestureExclusionRects() (API 29+) - đúng kỹ thuật các app có
 * vuốt cạnh riêng (trình duyệt vuốt tiến/lùi trong WebView...) vẫn dùng.
 */
class SwipeBackGestureHelper(
    private val activity: Activity,
    private val target: View,
    private val edgeSizeDp: Float = 24f,
    private val commitThreshold: Float = 0.35f,
    private val onProgress: (progress: Float) -> Unit = {},
    private val onCancelled: () -> Unit = {},
    private val onCommit: () -> Unit
) {
    private enum class State { IDLE, CANDIDATE, DRAGGING }

    private val edgeSizePx: Float = edgeSizeDp * activity.resources.displayMetrics.density
    private val touchSlop: Int = ViewConfiguration.get(activity).scaledTouchSlop
    private val minFlingVelocity: Float =
        ViewConfiguration.get(activity).scaledMinimumFlingVelocity.toFloat()

    private var state = State.IDLE
    private var startX = 0f
    private var startY = 0f
    private var velocityTracker: VelocityTracker? = null

    private val layoutChangeListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateGestureExclusionRects() }

    /**
     * Khai báo vùng mép trái là "gesture exclusion" để hệ thống không cướp mất sự kiện chạm ở
     * đó cho cử chỉ back của hệ thống (chỉ có tác dụng từ Android 10 trở lên - các bản cũ hơn
     * dùng điều hướng nút bấm, không có kiểu "cướp cử chỉ" này nên không cần xử lý gì thêm).
     * Gọi 1 lần sau khi setContentView() và tự động cập nhật lại mỗi khi layout đổi (xoay màn
     * hình, đổi kích thước cửa sổ...).
     */
    fun installGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        target.addOnLayoutChangeListener(layoutChangeListener)
        if (target.isLaidOut) {
            updateGestureExclusionRects()
        }
    }

    fun uninstallGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        target.removeOnLayoutChangeListener(layoutChangeListener)
        target.systemGestureExclusionRects = emptyList()
    }

    private fun updateGestureExclusionRects() {
        val height = target.height
        if (height <= 0) return
        // Dải theo SÁT MÉP TRÁI, cao bằng toàn bộ view - đúng vùng mà dispatchTouchEvent() bên
        // dưới dùng để nhận diện điểm bắt đầu vuốt (edgeSizePx).
        val rect = Rect(0, 0, edgeSizePx.toInt().coerceAtLeast(1), height)
        target.systemGestureExclusionRects = listOf(rect)
    }

    /**
     * Gọi ở ĐẦU Activity.dispatchTouchEvent(ev), trước super.dispatchTouchEvent(ev).
     * Trả về true nghĩa là helper đã tự xử lý xong sự kiện này - Activity KHÔNG được forward
     * cho super nữa. Trả về false nghĩa là cứ gọi super.dispatchTouchEvent(ev) như bình thường.
     */
    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.rawX
                startY = ev.rawY
                recycleVelocityTracker()
                state = if (ev.x <= edgeSizePx) {
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(ev) }
                    State.CANDIDATE
                } else {
                    State.IDLE
                }
                // Luôn để DOWN đi xuống bình thường - nếu cuối cùng chỉ là 1 cái chạm (tap)
                // thông thường thì item vẫn nhận ripple/click y như chưa có helper này.
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (state == State.IDLE) return false
                velocityTracker?.addMovement(ev)

                val dx = ev.rawX - startX
                val dy = ev.rawY - startY

                if (state == State.CANDIDATE) {
                    if (dx < 0 || abs(dy) > abs(dx)) {
                        // Vuốt sai hướng (kéo ngược lại) hoặc chủ yếu theo chiều dọc (đang cuộn
                        // danh sách) -> không phải cử chỉ back, bỏ theo dõi, để cuộn/click tiếp
                        // tục hoạt động bình thường như chưa có helper.
                        state = State.IDLE
                        recycleVelocityTracker()
                        return false
                    }
                    if (dx < touchSlop) return false // chưa đủ xa để chắc chắn là vuốt back

                    // Đủ điều kiện xác nhận đây là cử chỉ vuốt back: huỷ chuỗi sự kiện đang dở
                    // trong cây view (bỏ trạng thái pressed của item / dừng cuộn dở của
                    // RecyclerView) bằng 1 ACTION_CANCEL giả, rồi từ giờ tự xử lý toàn bộ.
                    cancelOngoingChildTouch(ev)
                    state = State.DRAGGING
                }

                val width = target.width
                if (width > 0) {
                    val clampedDx = dx.coerceIn(0f, width.toFloat())
                    target.translationX = clampedDx
                    onProgress((clampedDx / width).coerceIn(0f, 1f))
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = state == State.DRAGGING
                state = State.IDLE
                if (!wasDragging) {
                    recycleVelocityTracker()
                    return false
                }

                val width = target.width.takeIf { it > 0 } ?: 1
                val dx = (ev.rawX - startX).coerceIn(0f, width.toFloat())
                val progress = dx / width

                var velocityX = 0f
                velocityTracker?.apply {
                    addMovement(ev)
                    computeCurrentVelocity(1000)
                    velocityX = xVelocity
                }
                recycleVelocityTracker()

                val shouldCommit = ev.actionMasked == MotionEvent.ACTION_UP &&
                        (progress >= commitThreshold || velocityX >= minFlingVelocity)
                finishDrag(commit = shouldCommit)
                return true
            }
        }
        return false
    }

    /**
     * Gửi 1 ACTION_CANCEL giả thẳng xuống cây view (bỏ qua Activity.dispatchTouchEvent để
     * không gọi ngược lại chính helper này) để item/RecyclerView huỷ trạng thái pressed/cuộn
     * dở, tương tự cách RecyclerView/ViewPager tự "cướp" sự kiện giữa chừng khi phát hiện đúng
     * hướng cuộn của nó.
     */
    private fun cancelOngoingChildTouch(current: MotionEvent) {
        val cancelEvent = MotionEvent.obtain(current)
        cancelEvent.action = MotionEvent.ACTION_CANCEL
        activity.window.superDispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()
        target.animate().cancel()
    }

    private fun finishDrag(commit: Boolean) {
        val width = target.width.toFloat()
        if (commit && width > 0) {
            target.animate()
                .translationX(width)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        target.animate().setListener(null)
                        onCommit()
                    }
                })
                .start()
        } else {
            target.animate()
                .translationX(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        target.animate().setListener(null)
                        onCancelled()
                    }
                })
                .start()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }
}
