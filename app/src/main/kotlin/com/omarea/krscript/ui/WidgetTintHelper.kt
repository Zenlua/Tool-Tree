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

// Tự động tô màu cho kr_widget (icon phụ ở góc phải các item action/page/picker/editor):
// - Nếu item có icon riêng (kr_icon, config.iconPath) -> lấy màu TRUNG BÌNH của icon đó áp
//   vào kr_widget, để icon phụ "hoà" theo tông màu icon chính thay vì luôn 1 màu cố định.
// - Nếu item KHÔNG có icon riêng -> giữ nguyên ?android:attr/colorAccent như mặc định
//   khai báo sẵn trong kr_action_list_item.xml.
object WidgetTintHelper {
    fun applyTint(context: Context, widgetView: ImageView?, iconDrawable: Drawable?) {
        widgetView ?: return
        val color = iconDrawable?.let { extractAverageColor(it) } ?: resolveAccentColor(context)
        widgetView.imageTintList = ColorStateList.valueOf(color)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    // Thu nhỏ icon về lưới nhỏ (12x12) rồi lấy trung bình RGB, bỏ qua pixel gần như
    // trong suốt (nền) để màu không bị pha loãng bởi vùng trống của icon.
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
