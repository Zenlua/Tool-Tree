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
 * capture() ngay trước khi startActivity() (kết quả trả về qua callback vì dùng PixelCopy -
 * xem giải thích bên dưới - nên phải startActivity() BÊN TRONG callback đó), rồi bên trang
 * mới gọi consume() 1 lần trong onCreate().
 *
 * Vì sao dùng PixelCopy thay vì decorView.draw(canvas) như trước: các item trong danh sách
 * (BlurViewLinearLayout, xem BlurEngine.setup()) tự bo góc lớp kính mờ của nó bằng
 * setClipToOutline(true) - đây là clip cấp RenderNode, chỉ được áp dụng khi View được render
 * qua pipeline hardware bình thường trên màn hình. Gọi thủ công decorView.draw(canvas) để
 * chụp vào 1 Bitmap software HOÀN TOÀN BỎ QUA clip này ở mọi cấp (cả khung ngoài lẫn từng
 * item riêng) - nên ảnh chụp ra bị "vuông góc" dù trên màn hình đang hiển thị bo tròn.
 * PixelCopy.request() chụp thẳng từ buffer đã render thật (GPU composite) nên giữ đúng mọi
 * clip/outline như mắt thấy trên màn hình.
 */
object SwipeBackPreviewCache {

    class Preview(val sharp: Bitmap, val blurred: Bitmap?)

    private var preview: Preview? = null

    // Chụp gần như đúng độ phân giải màn hình để ảnh preview không bị vỡ/mờ khi phóng lại
    // đúng kích thước lúc hiển thị - chỉ giảm nhẹ 1 chút để tiết kiệm bộ nhớ/CPU
    private const val SCALE = 1f

    /**
     * Chụp lại nội dung đang hiển thị thật sự của activity hiện tại (qua PixelCopy, giữ đúng
     * bo góc từng item lẫn khung ngoài). Nên gọi ngay trước khi startActivity() để nội dung
     * chụp được là khung hình mới nhất, tránh chụp phải giao diện đang dở dang.
     *
     * Bất đồng bộ (PixelCopy chỉ hoạt động kiểu callback) - onCaptured() luôn được gọi đúng 1
     * lần khi xong (dù thành công, thất bại, hay lỗi ngoại lệ), nơi gọi PHẢI startActivity()
     * bên trong onCaptured() thay vì ngay sau capture().
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
                        // PixelCopy thất bại (hiếm, ví dụ window đang chuyển trạng thái) -> vẫn
                        // còn hơn không có ảnh gì, chấp nhận mất bo góc trong trường hợp hiếm
                        // này thay vì bỏ hẳn hiệu ứng preview
                        drawFallback(decorView, sharp)
                    }
                    finishCapture(sharp, onCaptured)
                }, Handler(Looper.getMainLooper()))
            } else {
                // API < 26 không có PixelCopy(Window) - giữ cách chụp cũ (chấp nhận không giữ
                // được bo góc, nhưng đây là dải thiết bị cũ, ít quan trọng hơn)
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
        // Bản mờ dùng lại đúng thuật toán blur nền đang có sẵn của app (StackBlur, tự thu nhỏ
        // 10% trước khi làm mờ nên rất nhanh) - để phong cách nhất quán với hiệu ứng blur nền
        // khác trong app
        val blurred = try {
            FastBlurUtility.startBlurBackground(sharp)
        } catch (_: Exception) {
            null
        }

        recycle()
        preview = Preview(sharp, blurred)
        onCaptured()
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