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

        // 1. KHÔNG có icon -> Trả về màu gốc mặc định của layout (không tô tint)
        if (iconDrawable == null) {
            widgetView.imageTintList = null
            return
        }

        // 2. CÓ icon -> Trích xuất màu trung bình và tăng độ sáng tươi
        val rawColor = extractAverageColor(iconDrawable) ?: resolveAccentColor(context)
        val brightColor = maximizeBrightness(rawColor)
        
        widgetView.imageTintList = ColorStateList.valueOf(brightColor)
    }

    /**
     * Tăng độ sáng lên 100% (Value = 1.0f).
     * Đảm bảo Saturation ở mức phù hợp (>= 0.4f) để khi tăng sáng màu không bị biến thành trắng tinh.
     */
    private fun maximizeBrightness(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // hsv[0]: Hue (sắc độ)
        // hsv[1]: Saturation (độ bão hòa màu 0.0 -> 1.0)
        // hsv[2]: Value (độ sáng 0.0 -> 1.0)

        hsv[2] = 1.0f // Đẩy độ sáng lên tối đa

        // Nếu độ bão hòa quá thấp (màu gốc hơi xám/nhạt), nâng nhẹ Saturation
        // để màu tô cho widget vẫn giữ được sắc tố rõ ràng thay vì ra màu trắng
        if (hsv[1] < 0.4f) {
            hsv[1] = 0.5f
        }

        return Color.HSVToColor(hsv)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    private fun extractAverageColor(drawable: Drawable): Int? {
        val sampleSize = 12
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

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) < 32) continue
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
