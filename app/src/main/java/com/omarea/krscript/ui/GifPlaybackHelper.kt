package com.omarea.krscript.ui

import android.graphics.drawable.AnimationDrawable
import android.widget.ImageView

// Điều khiển việc phát hoạt ảnh kiểu GIF (AnimationDrawable) cho ImageView,
// dùng chung cho ListItemClickable (icon/photo) và ListItemText (photo trong dòng text).
object GifPlaybackHelper {
    /**
     * Gắn hoạt ảnh vào ImageView.
     * - autoplay = true: tự chạy ngay khi view attach vào window.
     * - autoplay = false: không tự chạy, người dùng bấm vào ảnh để phát/tạm dừng.
     * - loopCount > 0: dừng lại sau đúng số vòng lặp đó (dừng ở khung gần cuối của vòng cuối).
     * - loopCount <= 0: lặp vô hạn (mặc định).
     * Nếu drawable hiện tại không phải AnimationDrawable thì không làm gì (ảnh tĩnh bình thường).
     */
    fun bind(imageView: ImageView?, autoplay: Boolean, loopCount: Int) {
        val drawable = imageView?.drawable as? AnimationDrawable ?: return

        if (autoplay) {
            imageView.isClickable = false
            imageView.setOnClickListener(null)
            imageView.post {
                if (imageView.drawable === drawable) {
                    startWithLoopLimit(imageView, drawable, loopCount)
                }
            }
        } else {
            // Không tự chạy: hiện khung hình đầu tiên, chờ người dùng bấm vào để phát/tạm dừng
            imageView.isClickable = true
            imageView.setOnClickListener {
                if (drawable.isRunning) {
                    drawable.stop()
                } else {
                    startWithLoopLimit(imageView, drawable, loopCount)
                }
            }
        }
    }

    private fun startWithLoopLimit(imageView: ImageView, drawable: AnimationDrawable, loopCount: Int) {
        drawable.stop()
        drawable.start()
        if (loopCount > 0) {
            var totalDuration = 0L
            for (i in 0 until drawable.numberOfFrames) {
                totalDuration += drawable.getDuration(i)
            }
            val stopAfterMs = totalDuration * loopCount
            if (stopAfterMs > 0) {
                imageView.postDelayed({
                    if (imageView.drawable === drawable) {
                        drawable.stop()
                    }
                }, stopAfterMs)
            }
        }
    }
}
