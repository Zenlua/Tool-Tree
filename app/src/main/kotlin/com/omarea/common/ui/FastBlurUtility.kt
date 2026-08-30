package com.omarea.common.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

object FastBlurUtility {

    // Tỉ lệ thu nhỏ ảnh để xử lý nhanh (1/10 giúp giảm 100 lần số pixel cần tính toán)
    private const val SCALE_FACTOR = 0.15f
    private const val BLUR_RADIUS = 10

    /**
     * Chụp màn hình và làm mờ (Dùng làm phương án dự phòng khi không lấy được Wallpaper)
     */
    @JvmStatic
    fun getBlurBackgroundDrawer(activity: Activity): Bitmap? {
        val bmp = takeScreenShot(activity)
        return startBlurBackground(bmp)
    }

    /**
     * Quy trình xử lý: Thu nhỏ -> Làm mờ -> Phóng to & Nhuộm tối (Dim)
     * Đảm bảo mượt mà từ SDK 23 trở lên.
     */
    @JvmStatic
    fun startBlurBackground(bkg: Bitmap?): Bitmap? {
        if (bkg == null || bkg.isRecycled) return null

        // 1. Tính toán kích thước thu nhỏ
        val width = Math.round(bkg.width * SCALE_FACTOR)
        val height = Math.round(bkg.height * SCALE_FACTOR)

        if (width <= 0 || height <= 0) return bkg

        return try {
            // 2. Thu nhỏ ảnh (Sử dụng bộ lọc Bilinear để ảnh mượt hơn)
            val smallBitmap = Bitmap.createScaledBitmap(bkg, width, height, true)

            // 3. Làm mờ bằng thuật toán StackBlur (CPU-based, cực kỳ ổn định)
            val blurred = fastBlur(smallBitmap, BLUR_RADIUS)

            // FIX: smallBitmap chỉ là ảnh trung gian, luôn giải phóng sau khi dùng xong
            // (trừ trường hợp createScaledBitmap trả về chính bkg do width/height không đổi)
            if (smallBitmap !== bkg && !smallBitmap.isRecycled) {
                smallBitmap.recycle()
            }

            // FIX: fastBlur() có thể trả về null (radius < 1), phải kiểm tra trước khi dùng
            if (blurred == null || blurred.isRecycled) {
                return bkg
            }

            // 4. Phóng to về kích thước gốc và áp dụng bộ lọc màu tối
            scaleAndDim(blurred, bkg.width, bkg.height)
        } catch (e: OutOfMemoryError) {
            bkg
        }
    }

    /**
     * Chụp ảnh màn hình an toàn trên SDK 23+
     */
    private fun takeScreenShot(activity: Activity): Bitmap? {
        return try {
            val view: View = activity.window.decorView
            if (view.width <= 0 || view.height <= 0) return null

            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Phóng to ảnh và áp dụng ColorMatrix để làm tối nền (Dim)
     */
    private fun scaleAndDim(bitmap: Bitmap?, targetW: Int, targetH: Int): Bitmap? {
        if (bitmap == null || bitmap.isRecycled) return null

        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Paint với bộ lọc chống răng cưa và lọc bitmap khi scale
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        // Tạo bộ lọc màu để giảm độ sáng (contrast 0.85f ~ giảm 15% độ sáng)
        val cm = ColorMatrix()
        val contrast = 0.80f
        cm.set(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)

        // Vẽ ảnh từ vùng nguồn (nhỏ) ra vùng đích (toàn màn hình)
        val src = Rect(0, 0, bitmap.width, bitmap.height)
        val dst = Rect(0, 0, targetW, targetH)
        canvas.drawBitmap(bitmap, src, dst, paint)

        // Giải phóng bitmap tạm sau khi đã vẽ xong
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }

        return output
    }

    /**
     * Thuật toán StackBlur (Multi-pass box blur) - Tối ưu cho hiệu năng CPU
     * Hỗ trợ hoàn hảo cho các thiết bị từ cũ đến mới.
     */
    private fun fastBlur(sentBitmap: Bitmap?, radius: Int): Bitmap? {
        if (sentBitmap == null || sentBitmap.isRecycled || radius < 1) return null

        // FIX: getConfig() có thể null với bitmap dạng HARDWARE -> copy() sẽ ném exception
        val config = sentBitmap.config ?: Bitmap.Config.ARGB_8888
        val bitmap = sentBitmap.copy(config, true)

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = (i / divsum)
            i++
        }

        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        lateinit var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            rinsum = 0; ginsum = 0; binsum = 0; routsum = 0; goutsum = 0; boutsum = 0; rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) vmin[y] = min(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
