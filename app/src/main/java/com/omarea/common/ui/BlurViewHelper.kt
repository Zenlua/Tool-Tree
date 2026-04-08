package com.omarea.common.ui

import android.graphics.*
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity

class BlurHelper(private val view: View) {

    companion object {
        @JvmField var blurBackground: Bitmap? = null // Ảnh nền toàn màn hình đã được làm mờ sẵn
        @JvmField var isBlurEnabled = true
        private const val SCALE_REFERENCE = 192f // Tỉ lệ nén giống mã gốc
    }

    private var lastLocation = intArrayOf(Int.MIN_VALUE, Int.MIN_VALUE)
    private var lastSize = arrayOf(0, 0)
    private var croppedBitmap: Bitmap? = null

    fun getCroppedBitmap(): Bitmap? = if (isBlurEnabled) croppedBitmap else null

    fun init() {
        // Lắng nghe sự kiện vẽ để cập nhật frame mờ
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (view.isAttachedToWindow) update()
                return true
            }
        })
    }

    fun update() {
        val bg = blurBackground ?: return
        
        if (isBlurEnabled && view.width > 0 && view.height > 0 && view.isAttachedToWindow) {
            val context = view.context as? AppCompatActivity ?: return
            
            // Bỏ qua nếu ở chế độ đa cửa sổ (tọa độ sẽ bị sai)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (context.isInMultiWindowMode || context.isInPictureInPictureMode) return
            }

            val location = intArrayOf(0, 0)
            view.getLocationInWindow(location)

            // Chỉ tính toán lại khi View di chuyển hoặc đổi kích thước
            if (lastLocation[0] == location[0] && lastLocation[1] == location[1] &&
                lastSize[0] == view.width && lastSize[1] == view.height) {
                return
            }

            lastLocation = location
            lastSize = arrayOf(view.width, view.height)

            try {
                val scale = view.rootView.width.toFloat() / SCALE_REFERENCE
                val sw = (view.width / scale).toInt().coerceAtLeast(1)
                val sh = (view.height / scale).toInt().coerceAtLeast(1)
                val ox = (location[0] / scale).toInt()
                val oy = (location[1] / scale).toInt()

                if (croppedBitmap == null || croppedBitmap?.width != sw || croppedBitmap?.height != sh) {
                    croppedBitmap?.recycle()
                    croppedBitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                }

                croppedBitmap?.let { out ->
                    val canvas = Canvas(out)
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    
                    val src = Rect(
                        ox.coerceAtLeast(0),
                        oy.coerceAtLeast(0),
                        (ox + sw).coerceAtMost(bg.width),
                        (oy + sh).coerceAtMost(bg.height)
                    )
                    val dest = Rect(0, 0, src.width(), src.height())
                    canvas.drawBitmap(bg, src, dest, null)
                    
                    view.invalidate() // Yêu cầu vẽ lại lớp BlurViewLinearLayout
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}
