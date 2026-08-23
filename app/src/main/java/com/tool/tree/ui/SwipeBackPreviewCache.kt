package com.tool.tree.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import com.omarea.common.ui.FastBlurUtility

/**
 * Nơi lưu tạm ảnh chụp nhanh màn hình hiện tại (bản nét + bản đã làm mờ), để trang được mở ra
 * tiếp theo (ActionPage) có thể hiển thị nó làm "hình nền" phía sau trong lúc người dùng vuốt
 * để trở lại - tạo cảm giác giống như đang thấy cửa sổ/trang trước đó thật sự, dù thực chất cả
 * 2 vẫn nằm trong cùng 1 activity/window (không dùng windowIsTranslucent để tránh đụng vào hệ
 * thống nền/blur hình nền đang có sẵn của app).
 *
 * Giữ cả bản mờ lẫn bản nét để trang sau có thể crossfade giữa 2 bản này theo tiến độ vuốt:
 * vuốt càng nhiều -> bản nét càng hiện rõ (giống hiệu ứng "lấy nét dần" khi kéo cửa sổ cũ ra).
 *
 * Chỉ giữ ĐÚNG 1 cặp ảnh tại 1 thời điểm (dùng cho lần mở trang kế tiếp gần nhất) - gọi
 * capture() ngay trước khi startActivity(), rồi bên trang mới gọi consume() 1 lần trong
 * onCreate().
 */
object SwipeBackPreviewCache {

    class Preview(val sharp: Bitmap, val blurred: Bitmap?)

    private var preview: Preview? = null

    // Chụp gần như đúng độ phân giải màn hình để ảnh preview không bị vỡ/mờ khi phóng lại
    // đúng kích thước lúc hiển thị - chỉ giảm nhẹ 1 chút để tiết kiệm bộ nhớ/CPU cho thao
    // tác chụp diễn ra ngay trên main thread (trước khi mở activity mới)
    private const val SCALE = 1f

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
                recycle()
                return
            }

            val scaledWidth = (width * SCALE).toInt().coerceAtLeast(1)
            val scaledHeight = (height * SCALE).toInt().coerceAtLeast(1)

            val sharp = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sharp)
            canvas.scale(SCALE, SCALE)
            decorView.draw(canvas)

            // Bản mờ dùng lại đúng thuật toán blur nền đang có sẵn của app (StackBlur, tự thu
            // nhỏ 10% trước khi làm mờ nên rất nhanh, không lo giật khi chạy đồng bộ ở đây) -
            // để phong cách nhất quán với hiệu ứng blur hình nền khác trong app
            val blurred = try {
                FastBlurUtility.startBlurBackground(sharp)
            } catch (_: Exception) {
                null
            }

            recycle()
            preview = Preview(sharp, blurred)
        } catch (_: Exception) {
            recycle()
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
