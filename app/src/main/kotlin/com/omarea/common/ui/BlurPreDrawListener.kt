package com.omarea.common.ui

import android.graphics.Bitmap
import android.view.View
import android.view.ViewTreeObserver

class BlurPreDrawListener(private val engine: BlurEngine, private val targetView: View) :
    ViewTreeObserver.OnPreDrawListener {

    // Lưu lại trạng thái của lần invalidate gần nhất để phát hiện "có gì thay đổi
    // thật sự hay không" trước khi quyết định vẽ lại.
    private var lastBitmap: Bitmap? = null
    private var lastX = Int.MIN_VALUE
    private var lastY = Int.MIN_VALUE
    private val location = IntArray(2)

    override fun onPreDraw(): Boolean {
        if (!targetView.isShown || BlurEngine.isPaused || BlurEngine.blurBitmap == null) {
            return true
        }

        // QUAN TRỌNG: onPreDraw() được đăng ký trên ViewTreeObserver của chính
        // targetView, nên gọi invalidate() vô điều kiện ở đây sẽ tự lên lịch một
        // pass vẽ mới, pass đó lại kích hoạt onPreDraw lần nữa -> vòng lặp vẽ lại
        // vô hạn ở tốc độ tối đa dù màn hình hoàn toàn tĩnh. Vì bitmap nền
        // (BlurEngine.blurBitmap) trên thực tế chỉ đổi khi wallpaper/theme đổi
        // (xem BlurController.captureAndBlur), ta chỉ cần invalidate khi:
        //   1) bitmap nền vừa được cập nhật, hoặc
        //   2) vị trí của targetView trên màn hình vừa đổi (đang cuộn/vuốt/animate).
        // Nếu không có gì đổi thì dừng vòng lặp tại đây, không invalidate nữa.
        //
        // LƯU Ý: không được throttle tay (giới hạn ví dụ ~30fps) ở nhánh
        // locationChanged, vì lớp blur phải bám đúng vị trí view mỗi khung hình.
        // Nếu throttle xuống dưới tốc độ làm mới màn hình, lớp blur sẽ bị trễ so
        // với vị trí thật khi vuốt nhanh, gây lệch/giật hình rõ rệt. Việc gộp
        // nhiều lệnh invalidate() trong cùng 1 khung hình vsync đã được chính hệ
        // thống Android xử lý sẵn, nên không cần tự giới hạn tần suất ở đây.

        val currentBitmap = BlurEngine.blurBitmap
        val bitmapChanged = currentBitmap !== lastBitmap

        targetView.getLocationOnScreen(location)
        val locationChanged = location[0] != lastX || location[1] != lastY

        if (!bitmapChanged && !locationChanged) {
            return true
        }

        lastBitmap = currentBitmap
        lastX = location[0]
        lastY = location[1]

        targetView.invalidate()
        return true
    }
}
