package com.omarea.common.ui

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max
import kotlin.math.round

/**
 * Tiện ích tạo ảnh nền mờ cho Dialog.
 *
 * Ưu tiên lấy bitmap blur đã được cache bởi BlurController (RenderScript, chất lượng cao,
 * đã bao gồm wallpaper/contrast/tint) → chỉ cần scale lên full-screen, cực nhanh.
 *
 * Fallback: nếu chưa có cache, chụp screenshot + blur bằng RenderScript (thông qua
 * BlurController.controller) thay vì StackBlur CPU cũ (thường bị fail trên một số thiết bị).
 *
 * So sánh với bản cũ (StackBlur):
 *   - RenderScript: GPU-accelerated, ổn định hơn, blur mượt hơn
 *   - Ưu tiên cache: tránh chụp + blur lại mỗi lần mở dialog
 *   - Scale 20% + radius 16f: chất lượng blur cao hơn hẳn bản 15% + radius 10 cũ
 *   - Contrast thích ứng dark/light mode thay vì cố định 0.80f
 */
object FastBlurUtility {

    /**
     * Tạo ảnh nền mờ full-screen cho Dialog.
     *
     * Pipeline:
     *   1. Nếu BlurEngine.blurBitmap đã có (BlurController đã xử lý trước đó) →
     *      scale lên full-screen + tint → trả về ngay (không cần chụp/blur lại).
     *   2. Nếu chưa có → chụp screenshot → blur bằng RenderScript (BlurController) →
     *      tint → trả về.
     *
     * @return bitmap full-screen đã blur + tint, hoặc null nếu thất bại.
     *         Caller phải recycle bitmap khi không còn dùng.
     */
    @JvmStatic
    fun getBlurBackgroundDrawer(activity: Activity): Bitmap? {
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val screenHeight = activity.resources.displayMetrics.heightPixels
        if (screenWidth <= 0 || screenHeight <= 0) return null

        // ─── Ưu tiên 1: Dùng blur bitmap đã cache bởi BlurController ───
        val cachedBlur = BlurEngine.blurBitmap
        if (cachedBlur != null && !cachedBlur.isRecycled) {
            return scaleWithTint(cachedBlur, screenWidth, screenHeight)
        }

        // ─── Ưu tiên 2: Chụp màn hình + blur bằng RenderScript ───
        val screenshot = takeScreenShot(activity)
        if (screenshot == null || screenshot.isRecycled) return null

        val result = blurViaController(activity, screenshot, screenWidth, screenHeight)
        // Screenshot chỉ là nguồn trung gian, recycle sau khi đã dùng xong
        if (!screenshot.isRecycled) {
            screenshot.recycle()
        }
        return result
    }

    /**
     * Blur trực tiếp 1 bitmap đã chụp sẵn từ nơi khác (không tự chụp screenshot của activity).
     *
     * Dùng cho SwipeBackPreviewCache: bitmap "sharp" đã được chụp qua PixelCopy (giữ đúng bo
     * góc/clip) - chỉ cần blur nó bằng pipeline RenderScript sẵn có, giữ nguyên kích thước gốc
     * của bitmap đầu vào.
     *
     * @return bitmap đã blur + tint (cùng kích thước với sourceBitmap), hoặc null nếu lỗi.
     *         Caller phải recycle bitmap khi không còn dùng.
     */
    @JvmStatic
    fun blurBitmap(activity: Activity, sourceBitmap: Bitmap): Bitmap? {
        if (sourceBitmap.isRecycled) return null
        return blurViaController(activity, sourceBitmap, sourceBitmap.width, sourceBitmap.height)
    }

    /**
     * Chụp ảnh màn hình an toàn.
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
     * Scale bitmap blur (nhỏ, ~20% screen) lên full-screen và phủ tint.
     *
     * Dùng cho cả 2 trường hợp:
     *   - Bitmap cache từ BlurEngine.blurBitmap
     *   - Bitmap blur vừa tạo mới
     *
     * @return bitmap mới full-screen, hoặc null nếu lỗi. Caller phải recycle.
     */
    private fun scaleWithTint(blurBitmap: Bitmap, targetW: Int, targetH: Int): Bitmap? {
        return try {
            val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

            // Áp dụng contrast + tint giống BlurController
            val contrastValue = if (com.tool.tree.ThemeModeState.isDarkMode()) 0.9f else 1.2f
            val offset = (1f - contrastValue) * 128f
            val cm = ColorMatrix(
                floatArrayOf(
                    contrastValue, 0f, 0f, 0f, offset,
                    0f, contrastValue, 0f, 0f, offset,
                    0f, 0f, contrastValue, 0f, offset,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            paint.colorFilter = ColorMatrixColorFilter(cm)

            canvas.drawBitmap(blurBitmap, null, RectF(0f, 0f, targetW.toFloat(), targetH.toFloat()), paint)
            output
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Blur screenshot bằng RenderScript thông qua BlurController.
     *
     * Thay vì dùng StackBlur CPU (bản cũ, hay fail), gọi trực tiếp vào
     * BlurController để tận dụng:
     *   - RenderScript GPU-accelerated (nhanh + ổn định hơn)
     *   - RS context/script đã cache (không tốn thời gian tạo lại)
     *   - Scale 20% + radius 16f (chất lượng cao hơn bản cũ)
     *
     * @return bitmap full-screen đã blur + tint, hoặc null. Caller phải recycle.
     */
    private fun blurViaController(activity: Activity, screenshot: Bitmap, screenWidth: Int, screenHeight: Int): Bitmap? {
        return try {
            // Scale xuống 20% giống BlurController (nhanh + đủ chất lượng)
            val scale = 0.20f
            val width = max(round(screenshot.width * scale).toInt(), 1)
            val height = max(round(screenshot.height * scale).toInt(), 1)
            val scaled = Bitmap.createScaledBitmap(screenshot, width, height, true)

            // Blur bằng RenderScript (BlurController.cacheBlurBitmap)
            val blurred = BlurEngine.controller.cacheBlurBitmap(activity.applicationContext, scaled, 16f)

            // scaled đã dùng xong, recycle
            if (!scaled.isRecycled) {
                scaled.recycle()
            }

            if (blurred == null || blurred.isRecycled) return null

            // Scale lên full-screen + tint
            val result = scaleWithTint(blurred, screenWidth, screenHeight)

            // blurred là bitmap trung gian, recycle (trừ khi nó được gán vào BlurEngine.blurBitmap bên trong controller)
            // An toàn: chỉ recycle nếu không phải là bitmap đang được cache
            if (blurred !== BlurEngine.blurBitmap && !blurred.isRecycled) {
                blurred.recycle()
            }

            result
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
