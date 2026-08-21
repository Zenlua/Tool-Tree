package com.tool.tree

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
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
 * Cách hoạt động: bắt ACTION_DOWN trong phạm vi [edgeSizeDp] tính từ mép trái [target], theo
 * dõi ACTION_MOVE để tính % vuốt = khoảng cách kéo / chiều rộng view rồi áp translationX lên
 * [target] tương ứng - giống hệt cách res/anim/activity_close_exit.xml dịch chuyển view khi
 * đóng activity (translateX 0%p -> 100%p, alpha giữ nguyên). Vuốt được bao nhiêu % thì view
 * dịch chuyển bấy nhiêu %, đúng như hiệu ứng preview vuốt trở lại của Android.
 *
 * Khi buông tay: nếu đã vuốt đủ xa (>= [commitThreshold]) hoặc vuốt đủ nhanh (fling, dùng
 * VelocityTracker) thì animate nốt phần còn lại rồi gọi [onCommit] (thường là finish()); ngược
 * lại animate trượt view về vị trí gốc rồi gọi [onCancelled].
 */
class SwipeBackGestureHelper(
    activity: Activity,
    private val target: View,
    private val edgeSizeDp: Float = 24f,
    private val commitThreshold: Float = 0.35f,
    private val onProgress: (progress: Float) -> Unit = {},
    private val onCancelled: () -> Unit = {},
    private val onCommit: () -> Unit
) {
    private val edgeSizePx: Float = edgeSizeDp * activity.resources.displayMetrics.density
    private val minFlingVelocity: Float =
        ViewConfiguration.get(activity).scaledMinimumFlingVelocity.toFloat()

    private var tracking = false
    private var startX = 0f
    private var startY = 0f
    private var velocityTracker: VelocityTracker? = null

    fun attach() {
        target.setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun detach() {
        target.setOnTouchListener(null)
        recycleVelocityTracker()
        tracking = false
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x <= edgeSizePx) {
                    tracking = true
                    startX = event.rawX
                    startY = event.rawY
                    recycleVelocityTracker()
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                    target.animate().cancel()
                } else {
                    tracking = false
                }
                // Không consume ACTION_DOWN để không phá vỡ click/scroll bình thường của các
                // view con (RecyclerView, nút bấm...) khi người dùng không chạm đúng mép.
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!tracking) return false
                velocityTracker?.addMovement(event)

                val dx = event.rawX - startX
                val dy = event.rawY - startY
                if (dx < 0 || abs(dy) > abs(dx) * 1.5f) {
                    // Vuốt sai hướng (kéo ngược lại hoặc chủ yếu theo chiều dọc) -> bỏ theo dõi,
                    // trả view về vị trí gốc nếu đã dịch chuyển một phần.
                    finishTracking(commit = false)
                    return false
                }

                val width = target.width
                if (width <= 0) return false
                val clampedDx = dx.coerceAtMost(width.toFloat())
                target.translationX = clampedDx
                onProgress((clampedDx / width).coerceIn(0f, 1f))
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!tracking) return false

                val width = target.width.takeIf { it > 0 } ?: 1
                val dx = (event.rawX - startX).coerceIn(0f, width.toFloat())
                val progress = dx / width

                var velocityX = 0f
                velocityTracker?.apply {
                    addMovement(event)
                    computeCurrentVelocity(1000)
                    velocityX = xVelocity
                }

                val shouldCommit = event.actionMasked == MotionEvent.ACTION_UP &&
                        (progress >= commitThreshold || velocityX >= minFlingVelocity)

                finishTracking(commit = shouldCommit)
                return true
            }
        }
        return false
    }

    private fun finishTracking(commit: Boolean) {
        tracking = false
        recycleVelocityTracker()

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
