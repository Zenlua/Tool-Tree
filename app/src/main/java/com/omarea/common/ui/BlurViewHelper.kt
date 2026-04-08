package com.omarea.common.ui

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Lớp hỗ trợ xử lý logic làm mờ cho View
 */
class BlurViewHelper(private val view: View) {

    companion object {
        // Bitmap nền đã được làm mờ (phải được gán từ bên ngoài, ví dụ từ Activity/Service)
        var blurBackground: Bitmap? = null
        
        // Cấu hình mặc định
        var isBlurEnabled = true
        var defaultCornerRadius = 30.0f
        
        // Màu phủ nhẹ (Overlay) để tạo hiệu ứng kính thực hơn
        private val OVERLAY_COLOR = Color.parseColor("#20ffffff")
    }

    private var lastLocation = intArrayOf(Int.MIN_VALUE, Int.MIN_VALUE)
    private var lastSize = arrayOf(0, 0)
    private var croppedBitmap: Bitmap? = null

    /**
     * Khởi tạo các thuộc tính cơ bản cho View
     */
    fun init() {
        view.clipToOutline = true
        // Lắng nghe sự kiện trước khi vẽ để cập nhật khung hình mờ
        view.viewTreeObserver.addOnPreDrawListener {
            updateBlurFrame()
            true
        }
    }

    /**
     * Tính toán và cắt ảnh nền khớp với vị trí của View hiện tại
     */
    fun updateBlurFrame() {
        val bg = blurBackground ?: return
        
        if (isBlurEnabled && view.width > 0 && view.height > 0 && view.isAttachedToWindow) {
            val activity = view.context as? AppCompatActivity ?: return
            
            // Bỏ qua nếu đang ở chế độ đa cửa sổ (Multi-window) để tránh sai lệch tọa độ
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) return
            }

            val currentLocation = intArrayOf(0, 0)
            view.getLocationInWindow(currentLocation)

            // Tối ưu: Chỉ xử lý nếu View bị di chuyển hoặc đổi kích thước
            if (lastLocation[0] == currentLocation[0] && 
                lastLocation[1] == currentLocation[1] && 
                lastSize[0] == view.width && 
                lastSize[1] == view.height) {
                return
            }

            lastLocation = currentLocation
            lastSize = arrayOf(view.width, view.height)

            try {
                // Tỉ lệ nén để tăng hiệu suất (tương tự bản gốc dùng 192)
                val scaleFactor = view.rootView.width.toFloat() / 192f
                
                val scaledW = (view.width / scaleFactor).toInt()
                val scaledH = (view.height / scaleFactor).toInt()
                val offsetX = (currentLocation[0] / scaleFactor).toInt()
                val offsetY = (currentLocation[1] / scaleFactor).toInt()

                // Quản lý bộ nhớ Bitmap
                if (croppedBitmap == null || croppedBitmap?.width != scaledW || croppedBitmap?.height != scaledH) {
                    croppedBitmap?.recycle()
                    croppedBitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
                }

                croppedBitmap?.let { bitmap ->
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    
                    // Xác định vùng cần cắt trên ảnh nền lớn
                    val srcRect = Rect(
                        offsetX.coerceAtLeast(0),
                        offsetY.coerceAtLeast(0),
                        (offsetX + scaledW).coerceAtMost(bg.width),
                        (offsetY + scaledH).coerceAtMost(bg.height)
                    )
                    
                    val destRect = Rect(0, 0, srcRect.width(), srcRect.height())
                    canvas.drawBitmap(bg, srcRect, destRect, null)
                    
                    // Vẽ thêm một lớp màu phủ trắng mỏng để tạo hiệu ứng kính mờ
                    canvas.drawColor(OVERLAY_COLOR)
                    
                    // Cập nhật Background cho View
                    view.background = BitmapDrawable(view.resources, bitmap)
                }
            } catch (e: Exception) {
                // Tránh crash app nếu có lỗi xử lý đồ họa
                e.printStackTrace()
            }
        }
    }
}
