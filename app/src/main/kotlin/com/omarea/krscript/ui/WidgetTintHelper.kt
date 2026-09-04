package com.omarea.krscript.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView

object WidgetTintHelper {

    fun applyTint(context: Context, widgetView: ImageView?, iconDrawable: Drawable?) {
        widgetView ?: return

        // 1. Không có icon -> Giữ nguyên màu gốc mặc định của Widget (không tăng sáng / không tô tint)
        if (iconDrawable == null) {
            widgetView.imageTintList = null
            return
        }

        // 2. Có icon -> Trích xuất màu chủ đạo
        val rawColor = extractAverageColor(iconDrawable)
        val accentColor = resolveAccentColor(context)
        
        // Tăng độ sáng an toàn (không bị biến thành màu trắng)
        val finalColor = maximizeBrightnessSafe(rawColor ?: accentColor, accentColor)

        widgetView.imageTintList = ColorStateList.valueOf(finalColor)
    }

    /**
     * Tăng độ sáng nhưng chống cháy thành màu trắng tinh:
     * - Nếu màu có sắc tố (Saturation >= 15%): Giữ nguyên tông màu, nâng sáng lên 0.9f.
     * - Nếu màu là xám/đen/trắng (Saturation < 15%): Lấy màu Accent của app thế vào.
     */
    private fun maximizeBrightnessSafe(color: Int, fallbackAccent: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // hsv[0]: Hue (sắc độ 0..360)
        // hsv[1]: Saturation (độ bão hòa 0..1)
        // hsv[2]: Value (độ sáng 0..1)

        // Nếu màu trích xuất bị mất sắc tố (đen, xám, trắng) -> Dùng màu Accent để tránh biến thành màu trắng
        if (hsv[1] < 0.15f) {
            Color.colorToHSV(fallbackAccent, hsv)
        }

        // Đẩy độ sáng lên 0.95f (sáng tươi nhưng không bị mất màu)
        hsv[2] = 0.95f

        return Color.HSVToColor(hsv)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    /**
     * Lấy màu trung bình của Icon, tự động LỌC BỎ các pixel mờ, pixel trắng nền và pixel quá tối.
     */
    private fun extractAverageColor(drawable: Drawable): Int? {
        val sampleSize = 16
        val bitmap = try {
            Bitmap.createBitmap(sampleSize, sampleSize, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            return null
        }

        val canvas = Canvas(bitmap)
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(0, 0, sampleSize, sampleSize)
        drawable.draw(canvas)
        drawable.bounds = oldBounds

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var counted = 0

        val hsv = FloatArray(3)

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                
                // Lọc pixel trong suốt
                if (Color.alpha(pixel) < 50) continue

                Color.colorToHSV(pixel, hsv)
                
                // Bỏ qua các pixel trắng nền (Saturation < 0.1 & Value > 0.9) 
                // và pixel đen/tối (Value < 0.1) để không làm xỉn/trắng màu trung bình
                if (hsv[1] < 0.1f && hsv[2] > 0.9f) continue
                if (hsv[2] < 0.1f) continue

                totalR += Color.red(pixel)
                totalG += Color.green(pixel)
                totalB += Color.blue(pixel)
                counted++
            }
        }

        bitmap.recycle()

        if (counted == 0) return null
        return Color.rgb((totalR / counted).toInt(), (totalG / counted).toInt(), (totalB / counted).toInt())
    }
}
