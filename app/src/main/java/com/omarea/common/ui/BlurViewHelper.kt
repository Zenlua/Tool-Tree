package com.omarea.common.ui

import android.graphics.*
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity

/**
 * Lớp Helper xử lý logic cắt ảnh nền (Tương ứng với v31 trong mã gốc)
 * Đã được sửa đổi để không ghi đè background XML của View.
 */
class BlurHelper(private val view: View) {

    companion object {
        @JvmField var blurBackground: Bitmap? = null // Ảnh nền toàn màn hình đã blur
        @JvmField var isBlurEnabled = true
        @JvmField var cornerRadius = 30.0f // Có thể điều chỉnh qua code nếu cần
        
        // Tỉ lệ nén để tối ưu hiệu năng (giống bản gốc 192)
        private const val SCALE_REFERENCE = 192f
    }

    private var lastLocation = intArrayOf(Int.MIN_VALUE, Int.MIN_VALUE)
    private var lastSize = arrayOf(0, 0)
    private var croppedBitmap: Bitmap? = null

    /**
     * Trả về Bitmap đã được cắt để View vẽ trong onDraw
     */
    fun getCroppedBitmap(): Bitmap? {
        return if (isBlurEnabled) croppedBitmap else null
    }

    /**
     * Khởi tạo các listener (Tương đương hàm m() trong v31)
     */
    fun init() {
        // Thiết lập Outline để bo góc theo cornerRadius (nếu XML không bo)
        view.outlineProvider = ViewOutlineProvider.BACKGROUND
        view.clipToOutline = true
        
        // Lắng nghe sự kiện trước khi vẽ để cập nhật khung hình
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (view.isAttachedToWindow) {
                    update()
                }
                return true
            }
        })
    }

    /**
     * Logic tính toán tọa độ và trích xuất phần ảnh mờ (Tương đương hàm n() trong v31)
     */
    fun update() {
        val bg = blurBackground ?: return
        
        if (isBlurEnabled && view.width > 0 && view.height > 0 && view.isAttachedToWindow) {
            val context = view.context as? AppCompatActivity ?: return
            
            // Bỏ qua nếu đang ở chế độ đặc biệt của Android (Multi-window)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (context.isInMultiWindowMode || context.isInPictureInPictureMode) return
            }

            val location = intArrayOf(0, 0)
            view.getLocationInWindow(location)

            // Kiểm tra xem vị trí hoặc kích thước có thay đổi không để tránh tính toán thừa
            if (lastLocation[0] == location[0] && lastLocation[1] == location[1] &&
                lastSize[0] == view.width && lastSize[1] == view.height) {
                return
            }

            lastLocation = location
            lastSize = arrayOf(view.width, view.height)

            try {
                // Tính toán tỉ lệ scale dựa trên độ phân giải màn hình
                val scale = view.rootView.width.toFloat() / SCALE_REFERENCE
                
                val sw = (view.width / scale).toInt().coerceAtLeast(1)
                val sh = (view.height / scale).toInt().coerceAtLeast(1)
                val ox = (location[0] / scale).toInt()
                val oy = (location[1] / scale).toInt()

                // Khởi tạo hoặc tái sử dụng Bitmap để tiết kiệm RAM
                if (croppedBitmap == null || croppedBitmap?.width != sw || croppedBitmap?.height != sh) {
                    croppedBitmap?.recycle()
                    croppedBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                }

                croppedBitmap?.let { out ->
                    val canvas = Canvas(out)
                    // Xóa sạch canvas cũ
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    
                    // Xác định vùng cắt trên ảnh blurBackground lớn
                    val src = Rect(
                        ox.coerceAtLeast(0),
                        oy.coerceAtLeast(0),
                        (ox + sw).coerceAtMost(bg.width),
                        (oy + sh).coerceAtMost(bg.height)
                    )
                    
                    val dest = Rect(0, 0, src.width(), src.height())
                    
                    // Vẽ phần ảnh nền vào bitmap nhỏ
                    canvas.drawBitmap(bg, src, dest, null)
                    
                    // Thông báo cho View thực hiện vẽ lại (gọi onDraw)
                    view.invalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Giải phóng bộ nhớ khi View bị hủy
     */
    fun destroy() {
        croppedBitmap?.recycle()
        croppedBitmap = null
    }
}
