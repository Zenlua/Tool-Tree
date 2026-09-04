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

        // 1. Không có icon -> Trả về null để giữ màu phủ mặc định gốc trong XML
        if (iconDrawable == null) {
            widgetView.imageTintList = null
            return
        }

        // 2. Trích xuất màu chủ đạo thực tế có trong icon
        val dominantColor = extractDominantColor(iconDrawable)

        if (dominantColor == null) {
            widgetView.imageTintList = null
            return
        }

        // 3. Tăng nhẹ độ sáng nếu màu bị tối
        val brightColor = brightenColor(dominantColor)

        widgetView.imageTintList = ColorStateList.valueOf(brightColor)
    }

    /**
     * Nâng nhẹ độ sáng (Value trong HSV) lên ngưỡng tối thiểu (0.8f)
     * giúp widget tươi sáng hơn mà không làm thay đổi hay bóp méo tông màu gốc.
     */
    private fun brightenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        // Nếu màu quá tối (Value < 0.8f), nâng nhẹ lên 0.8f
        if (hsv[2] < 0.9f) {
            hsv[2] = 0.9f
        }

        return Color.HSVToColor(hsv)
    }

    private fun resolveAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    /**
     * Tìm màu chủ đạo có mặt thực tế trong Icon:
     * - Gom nhóm màu (Quantization) để đếm tần suất pixel.
     * - Ưu tiên các màu đặc trưng (Saturated) có diện tích lớn trong icon.
     * - Tuyệt đối KHÔNG cộng trung bình RGB để tránh tạo màu lạ.
     */
    private fun extractDominantColor(drawable: Drawable?): Int? {
        drawable ?: return null

        val sampleSize = 24
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

        val colorScores = HashMap<Int, Float>()
        val hsv = FloatArray(3)

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = bitmap.getPixel(x, y)

                // Bỏ qua pixel trong suốt
                if (Color.alpha(pixel) < 50) continue

                Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val valVal = hsv[2]

                // Làm tròn màu (bước 16) để nhóm các điểm ảnh có tông màu gần giống nhau
                val r = (Color.red(pixel) shr 4) shl 4
                val g = (Color.green(pixel) shr 4) shl 4
                val b = (Color.blue(pixel) shr 4) shl 4
                val quantizedColor = Color.rgb(r, g, b)

                // Tính điểm: Màu xuất hiện nhiều + có sắc tố rõ ràng sẽ được ưu tiên cao nhất
                val weight = 1.0f + (sat * 2.0f) + (if (valVal > 0.15f && valVal < 0.95f) 0.5f else 0.0f)

                val currentScore = colorScores[quantizedColor] ?: 0f
                colorScores[quantizedColor] = currentScore + weight
            }
        }

        bitmap.recycle()

        // Trả về màu thực tế trong icon đạt điểm cao nhất
        return colorScores.maxByOrNull { it.value }?.key
    }
}
