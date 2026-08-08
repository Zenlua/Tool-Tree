package com.omarea.krscript.ui

import android.view.View

// Hiệu ứng chuyển động (mờ dần + phóng nhẹ) khi 1 View chuyển giữa ẩn (GONE) và hiện (VISIBLE).
// Dùng ViewPropertyAnimator có sẵn của Android, không cần thư viện ngoài, an toàn khi gọi nhiều lần liên tiếp.
object ViewAnimUtil {
    private const val DEFAULT_DURATION_MS = 200L
    private const val START_SCALE = 0.92f

    /**
     * @param visible true: hiện view (fade-in + scale-in); false: ẩn view (fade-out + scale-out rồi mới GONE)
     */
    @JvmStatic
    fun setVisibleAnimated(view: View?, visible: Boolean, durationMs: Long = DEFAULT_DURATION_MS) {
        view ?: return
        view.animate().cancel()

        if (visible) {
            if (view.visibility == View.VISIBLE && view.alpha >= 1f) return
            view.alpha = 0f
            view.scaleX = START_SCALE
            view.scaleY = START_SCALE
            view.visibility = View.VISIBLE
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(durationMs)
                .start()
        } else {
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.GONE
                return
            }
            view.animate()
                .alpha(0f)
                .scaleX(START_SCALE)
                .scaleY(START_SCALE)
                .setDuration(durationMs)
                .withEndAction {
                    view.visibility = View.GONE
                    view.alpha = 1f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                .start()
        }
    }
}
