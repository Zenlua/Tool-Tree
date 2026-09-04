package com.omarea.krscript.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView

// Tự động tô màu cho kr_widget theo icon chính (chỉ áp dụng khi có icon):
object WidgetTintHelper {

    fun applyTint(context: Context, widgetView: ImageView?, iconDrawable: Drawable?) {
        widgetView ?: return

        // Nếu không có icon (iconDrawable == null), xóa màu tô và giữ màu gốc
        if (iconDrawable == null) {
            widgetView.imageTintList = null
            return
        }

        // Nếu có icon, tiến hành trích xuất màu trung bình
        val rawColor = extractAverageColor(iconDrawable) ?: resolveAccentColor(context)
        val brightColor = maximizeBrightness(rawColor)
        
        widgetView.imageTintList = ColorStateList.valueOf(brightColor)
    }

    private fun maximizeBrightness(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        if (hsv[1] < 0.15f) {
            hsv[1] = 0.25f
        }
        hsv[2] = 1.0f
        
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
