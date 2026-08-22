package com.omarea.common.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.View
import android.widget.ScrollView
import java.lang.ref.WeakReference

/**
 * Nguồn blur ĐỘNG cho BlurEngine.DynamicBlurSource - dùng để làm hiệu ứng "kính mờ" hiện
 * đúng nội dung của 1 ScrollView đang trôi qua sau lưng toolbar (khác với BlurController vốn
 * chỉ blur ảnh wallpaper tĩnh).
 *
 * CÁCH HOẠT ĐỘNG:
 * ScrollView sau khi bỏ layout_marginTop (xem activity_action_page.xml + kr_action_list_fragment.xml)
 * sẽ có cùng hệ tọa độ (0,0) với toolbar - tức là dải nội dung từ y=0 đến y=captureHeightPx()
 * của CHÍNH ScrollView đó chính là phần đang bị toolbar che. Vì vậy chỉ cần gọi
 * scrollView.draw(canvas) (giữ nguyên cơ chế cuộn nội bộ của View) và clip lại đúng dải này,
 * KHÔNG cần tính toán vị trí trên màn hình như nhánh wallpaper.
 *
 * HIỆU NĂNG: Bitmap chụp được scale nhỏ (SCALE) trước khi vẽ (giảm số pixel cần xử lý), và
 * chỉ chụp/blur lại khi:
 *   1) đã cuộn quá 1 ngưỡng nhỏ (MIN_SCROLL_DELTA_PX) so với lần chụp trước, VÀ
 *   2) đã qua tối thiểu MIN_CAPTURE_INTERVAL_MS kể từ lần chụp gần nhất (giới hạn ~30fps).
 * Nếu không, trả lại kết quả đã cache - tránh vuốt nhanh làm giật máy yếu (đặc biệt các máy
 * MIUI/Xiaomi cũ mà app đang nhắm tới).
 */
class ScrollContentBlurSource(
    scrollView: ScrollView,
    hostView: View,
    private val captureHeightPx: () -> Int
) : BlurEngine.DynamicBlurSource {

    companion object {
        private const val SCALE = 0.18f
        private const val BLUR_RADIUS = 6
        private const val MIN_SCROLL_DELTA_PX = 12
        private const val MIN_CAPTURE_INTERVAL_MS = 32L
    }

    private val scrollViewRef = WeakReference(scrollView)
    private val hostRef = WeakReference(hostView)

    private var cachedResult: Bitmap? = null
    private var pendingRecapture = true
    private var lastCaptureTime = 0L

    init {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (Math.abs(scrollY - oldScrollY) >= MIN_SCROLL_DELTA_PX) {
                pendingRecapture = true
                // Toolbar (hostView) không tự biết nội dung phía sau vừa đổi - phải chủ động
                // invalidate() để kích hoạt lại onDraw() -> getUpdatedBlurBitmap() của nó.
                hostRef.get()?.invalidate()
            }
        }
    }

    // Gọi khi cần buộc chụp lại ngay (ví dụ sau khi đổi theme sáng/tối, đổi dữ liệu list).
    fun invalidateNow() {
        pendingRecapture = true
        hostRef.get()?.invalidate()
    }

    override fun getBlurSnapshot(): Bitmap? {
        if (!pendingRecapture) {
            return cachedResult
        }

        val now = SystemClock.uptimeMillis()
        if (cachedResult != null && now - lastCaptureTime < MIN_CAPTURE_INTERVAL_MS) {
            // Chưa tới hạn chụp lại - giữ nguyên frame cũ, KHÔNG reset pendingRecapture để
            // lần gọi kế tiếp (frame sau) vẫn thử chụp lại.
            return cachedResult
        }

        val scrollView = scrollViewRef.get() ?: return cachedResult
        val width = scrollView.width
        val captureHeight = captureHeightPx()
        if (width <= 0 || captureHeight <= 0) {
            return cachedResult
        }

        val scaledW = maxOf(1, Math.round(width * SCALE))
        val scaledH = maxOf(1, Math.round(captureHeight * SCALE))

        return try {
            val snapshot = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(snapshot)
            canvas.scale(SCALE, SCALE)
            // Chỉ lấy đúng dải bị toolbar che - nhờ clipRect, các item nằm ngoài dải này
            // (quickReject) sẽ được View bỏ qua khi vẽ, nên chi phí không phụ thuộc số item
            // trong toàn bộ danh sách.
            canvas.clipRect(0, 0, width, captureHeight)
            scrollView.draw(canvas)

            val blurred = FastBlurUtility.blurSmallBitmap(snapshot, BLUR_RADIUS)
            if (blurred !== snapshot && !snapshot.isRecycled()) {
                snapshot.recycle()
            }

            cachedResult?.let { if (!it.isRecycled()) it.recycle() }
            cachedResult = blurred ?: snapshot
            pendingRecapture = false
            lastCaptureTime = now
            cachedResult
        } catch (e: Exception) {
            cachedResult
        }
    }

    fun destroy() {
        cachedResult?.let { if (!it.isRecycled()) it.recycle() }
        cachedResult = null
    }
}
