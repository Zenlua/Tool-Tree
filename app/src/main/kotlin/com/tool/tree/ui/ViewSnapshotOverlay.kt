package com.tool.tree.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

// Chụp ảnh 1 View rồi phủ lên đúng vị trí/kích thước của chính View đó (dùng lại layoutParams
// gốc) - dùng để giữ nguyên giao diện cũ trong lúc nội dung bên dưới đang tải lại (vd: WebView
// goBack() khiến trang tải lại từ mạng, gây nháy hình vài ms trước khi render xong).
object ViewSnapshotOverlay {

    // Chụp bitmap của view theo đúng kích thước hiện tại trên màn hình.
    @JvmStatic
    fun capture(view: View): Bitmap? {
        val width = view.width
        val height = view.height
        if (width <= 0 || height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    // Phủ bitmap lên đúng vị trí/kích thước của target (dùng chung layoutParams của target),
    // thêm vào parent ngay sau target trong z-order để nằm đè lên trên.
    @JvmStatic
    fun show(parent: ViewGroup, target: View, bitmap: Bitmap): View {
        val overlay = ImageView(target.context).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = target.layoutParams
        }
        val targetIndex = parent.indexOfChild(target)
        parent.addView(overlay, targetIndex + 1)
        return overlay
    }

    // Gỡ overlay ra khi nội dung bên dưới đã tải xong.
    @JvmStatic
    fun remove(parent: ViewGroup, overlay: View?) {
        if (overlay != null && overlay.parent === parent) {
            parent.removeView(overlay)
        }
    }
}
