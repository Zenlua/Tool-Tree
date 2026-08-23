package com.tool.tree.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * Nơi lưu tạm 1 bitmap chụp nhanh màn hình hiện tại, để trang được mở ra tiếp theo (ActionPage)
 * có thể hiển thị nó làm "hình nền" phía sau trong lúc người dùng vuốt để trở lại - tạo cảm giác
 * giống như đang thấy cửa sổ/trang trước đó thật sự, dù thực chất cả 2 vẫn nằm trong cùng 1
 * activity/window (không dùng windowIsTranslucent để tránh đụng vào hệ thống nền/blur hình nền
 * đang có sẵn của app).
 *
 * Chỉ giữ ĐÚNG 1 bitmap tại 1 thời điểm (dùng cho lần mở trang kế tiếp gần nhất) - gọi capture()
 * ngay trước khi startActivity(), rồi bên trang mới gọi consume() 1 lần trong onCreate().
 */
object SwipeBackPreviewCache {
    private var bitmap: Bitmap? = null

    // Vẽ thu nhỏ lại để giảm chi phí capture + bộ nhớ - chỉ dùng làm ảnh nền mờ phía sau lúc
    // kéo trong thời gian ngắn nên không cần độ nét cao
    private const val SCALE = 0.5f

    /**
     * Chụp lại decorView của activity hiện tại. Nên gọi ngay trước khi startActivity() để nội
     * dung chụp được là khung hình mới nhất, tránh chụp phải giao diện đang dở dang.
     */
    fun capture(activity: Activity) {
        try {
            val decorView = activity.window.decorView
            val width = decorView.width
            val height = decorView.height
            if (width <= 0 || height <= 0) {
                bitmap = null
                return
            }

            val scaledWidth = (width * SCALE).toInt().coerceAtLeast(1)
            val scaledHeight = (height * SCALE).toInt().coerceAtLeast(1)

            val result = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(result)
            canvas.scale(SCALE, SCALE)
            decorView.draw(canvas)

            recycle()
            bitmap = result
        } catch (_: Exception) {
            bitmap = null
        }
    }

    /** Lấy bitmap đã chụp (nếu có) và xóa khỏi cache - chỉ dùng được 1 lần. */
    fun consume(): Bitmap? {
        val result = bitmap
        bitmap = null
        return result
    }

    private fun recycle() {
        bitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        bitmap = null
    }
}
