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

        // 1. Trích xuất màu chủ đạo (trả về null nếu iconDrawable == null)
        val rawColor = extractAverageColor(iconDrawable)
        val accentColor = resolveAccentColor(context)
        
        // 2. Tăng độ sáng an toàn (không bị biến thành màu trắng)
        val finalColor = maximizeBrightnessSafe(rawColor ?: accentColor, accentColor)

        widgetView.imageTintList = ColorStateList.valueOf(finalColor)
    }

    /**
     * Tăng độ sáng nhưng chống cháy thành màu trắng tinh:
     * - Nếu màu có sắc tố (Saturation >= 15%): Giữ nguyên tông màu, nâng sáng lên 0.95f.
     * - Nếu màu là xám/đen/trắng (Saturation < 15%): Lấy màu Accent của app thế vào.
     */
    private fun maximizeBrightnessSafe(color: Int, fallbackAccent: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Nếu màu trích xuất bị mất sắc tố (đen, xám, trắng) -> Dùng màu Accent
        if (hsv[1] < 0.15f) {
            Color.colorToHSV(fallbackAccent, hsv)
        }

        // Đẩy độ sáng lên 0.95f
        hsv[2] = 0.95f

        return Color.HSVToColor(hsv)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    /**
     * Lấy màu trung bình của Icon, chấp nhận Drawable? nullable.
     */
    private fun extractAverageColor(drawable: Drawable?): Int? {
        drawable ?: return null

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
                
                // Bỏ qua pixel trắng nền (Saturation < 0.1 & Value > 0.9) và pixel quá tối (Value < 0.1)
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
