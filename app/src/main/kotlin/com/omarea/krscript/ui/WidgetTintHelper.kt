package com.omarea.krscript.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.graphics.ColorUtils

object WidgetTintHelper {

    fun applyTint(context: Context, widgetView: ImageView?, iconDrawable: Drawable?) {
        widgetView ?: return

        // KHÔNG CÓ ICON: Bỏ tint động để ImageView dùng lại màu phủ (tint) gốc trong XML
        if (iconDrawable == null) {
            widgetView.imageTintList = null 
            return
        }

        // CÓ ICON: Trích xuất Top 2 màu chủ đạo từ icon và trộn lại
        val result = extractTopColorsWithScores(iconDrawable)

        val finalColor = when {
            // Có 2 màu chủ đạo: Trộn theo đúng tỷ lệ trọng số xuất hiện
            result.color1 != null && result.color2 != null -> {
                val totalScore = result.score1 + result.score2
                val ratio = if (totalScore > 0f) result.score2 / totalScore else 0.5f
                ColorUtils.blendARGB(result.color1, result.color2, ratio)
            }
            // Chỉ có 1 màu chủ đạo: Giữ nguyên 100% màu gốc đó
            result.color1 != null -> {
                result.color1
            }
            // Không bóc tách được màu: Bỏ tint động
            else -> null
        }

        widgetView.imageTintList = finalColor?.let { ColorStateList.valueOf(it) }
    }

    private data class ColorResult(
        val color1: Int? = null,
        val score1: Float = 0f,
        val color2: Int? = null,
        val score2: Float = 0f
    )

    private fun extractTopColorsWithScores(drawable: Drawable?): ColorResult {
        drawable ?: return ColorResult()

        val sampleSize = 24
        val bitmap = try {
            Bitmap.createBitmap(sampleSize, sampleSize, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            return ColorResult()
        }

        val canvas = Canvas(bitmap)
        val oldBounds = drawable.copyBounds()
        drawable.setBounds(0, 0, sampleSize, sampleSize)
        drawable.draw(canvas)
        drawable.bounds = oldBounds

        val colorScores = HashMap<Int, Float>()
        val hsv = FloatArray(3)

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) < 50) continue

                Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]

                val r = (Color.red(pixel) shr 4) shl 4
                val g = (Color.green(pixel) shr 4) shl 4
                val b = (Color.blue(pixel) shr 4) shl 4
                val quantizedColor = Color.rgb(r, g, b)

                val weight = 1.0f + (sat * 2.0f)
                colorScores[quantizedColor] = (colorScores[quantizedColor] ?: 0f) + weight
            }
        }

        bitmap.recycle()

        val sortedColors = colorScores.entries.sortedByDescending { it.value }
        val top1 = sortedColors.getOrNull(0)
        val top2 = sortedColors.getOrNull(1)

        return ColorResult(
            color1 = top1?.key,
            score1 = top1?.value ?: 0f,
            color2 = top2?.key,
            score2 = top2?.value ?: 0f
        )
    }
}
