package com.tool.tree.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import com.omarea.common.ui.FastBlurUtility
import java.util.concurrent.Executors

/**
 * Nơi lưu tạm ảnh chụp nhanh màn hình hiện tại (bản nét + bản đã làm mờ), để trang được mở ra
 * tiếp theo (ActionPage) có thể hiển thị nó làm "hình nền" phía sau trong lúc người dùng vuốt
 * để trở lại - tạo cảm giác giống như đang thấy cửa sổ/trang trước đó thật sự, dù thực chất cả
 * 2 vẫn nằm trong cùng 1 activity/window.
 */
object SwipeBackPreviewCache {

    class Preview(val sharp: Bitmap, @Volatile var blurred: Bitmap? = null)

    private var preview: Preview? = null
    private val executor = Executors.newSingleThreadExecutor()

    private const val SCALE = 1f

    /**
     * Chụp lại nội dung đang hiển thị thật sự của activity hiện tại qua PixelCopy.
     * Gọi onCaptured() ngay khi thu được ảnh nét (sharp) để khởi chạy Activity mới lập tức.
     * Quá trình blur ảnh nền (FastBlurUtility) được đẩy hoàn toàn sang background thread.
     */
    fun capture(activity: Activity, onCaptured: () -> Unit) {
        try {
            val window = activity.window
            val decorView = window.decorView
            val width = decorView.width
            val height = decorView.height
            if (width <= 0 || height <= 0) {
                recycle()
                onCaptured()
                return
            }

            val scaledWidth = (width * SCALE).toInt().coerceAtLeast(1)
            val scaledHeight = (height * SCALE).toInt().coerceAtLeast(1)
            val sharp = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(window, sharp, { copyResult ->
                    if (copyResult != PixelCopy.SUCCESS) {
                        drawFallback(decorView, sharp)
                    }
                    finishCapture(sharp, onCaptured)
                }, Handler(Looper.getMainLooper()))
            } else {
                drawFallback(decorView, sharp)
                finishCapture(sharp, onCaptured)
            }
        } catch (_: Exception) {
            recycle()
            onCaptured()
        }
    }

    private fun drawFallback(decorView: View, sharp: Bitmap) {
        try {
            val canvas = Canvas(sharp)
            canvas.scale(SCALE, SCALE)
            decorView.draw(canvas)
        } catch (_: Exception) {
        }
    }

    private fun finishCapture(sharp: Bitmap, onCaptured: () -> Unit) {
        recycle()
        
        val currentPreview = Preview(sharp, null)
        preview = currentPreview

        // 1. Phản hồi ngay lập tức để Main Thread mở Activity mới không bị khựng
        onCaptured()

        // 2. Chuyển tác vụ tính toán StackBlur đắt đỏ ra Background Thread
        executor.execute {
            val blurredResult = try {
                FastBlurUtility.startBlurBackground(sharp)
            } catch (_: Exception) {
                null
            }

            if (blurredResult != null) {
                currentPreview.blurred = blurredResult
            }
        }
    }

    /** Lấy ảnh đã chụp (nếu có) và xóa khỏi cache - chỉ dùng được 1 lần. */
    fun consume(): Preview? {
        val result = preview
        preview = null
        return result
    }

    private fun recycle() {
        preview?.let {
            if (!it.sharp.isRecycled) it.sharp.recycle()
            it.blurred?.takeIf { b -> !b.isRecycled }?.recycle()
        }
        preview = null
    }
}
