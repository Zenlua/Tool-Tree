package com.omarea.krscript.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView

// Tự động tô màu cho kr_widget:
// - Lấy màu TRUNG BÌNH của icon chính.
// - Đẩy độ sáng (Value trong không gian HSV) lên mức TỐI ĐA (100%) để màu luôn sáng nhất.
object WidgetTintHelper {
    fun applyTint(context: Context, widgetView: ImageView?, iconDrawable: Drawable?) {
        widgetView ?: return
        val rawColor = iconDrawable?.let { extractAverageColor(it) } ?: resolveAccentColor(context)
        
        // Đẩy độ sáng của màu lên tối đa
        val brightColor = maximizeBrightness(rawColor)
        
        widgetView.imageTintList = ColorStateList.valueOf(brightColor)
    }

    /**
     * Chuyển đổi màu sang hệ màu HSV và ép Value (độ sáng) thành 1.0f (100%)
     */
    private fun maximizeBrightness(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = 1.0f // hsv[0]: Hue, hsv[1]: Saturation, hsv[2]: Value (Brightness)
        return Color.HSVToColor(hsv)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    // Thu nhỏ icon về lưới nhỏ (12x12) rồi lấy trung bình RGB, bỏ qua pixel trong suốt.
    private fun extractAverageColor(drawable: Drawable): Int? {
        val bitmap = drawableToBitmap(drawable) ?: return null
        val sampleSize = 12
        val scaled = try {
            Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        } catch (e: Exception) {
            return null
        }

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var counted = 0

        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                if (Color.alpha(pixel) < 32) continue
                totalR += Color.red(pixel)
                totalG += Color.green(pixel)
                totalB += Color.blue(pixel)
                counted++
            }
        }
        if (scaled !== bitmap) scaled.recycle()

        if (counted == 0) return null
        return Color.rgb((totalR / counted).toInt(), (totalG / counted).toInt(), (totalB / counted).toInt())
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
