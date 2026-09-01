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
 * Tiện ích tạo ảnh nền mờ, dùng cho 2 tình huống RIÊNG BIỆT - KHÔNG dùng chung 1 pipeline
 * như bản cũ, vì bản chất nguồn ảnh khác nhau:
 *
 *   1. VÀO TRANG MỚI (getPageBlurBackground): trang mới chưa có nội dung gì để chụp, nền
 *      mờ ở đây thực chất là ẢNH WALLPAPER đã được BlurController chụp/blur SẴN 1 lần khi
 *      app khởi động / đổi theme (xem BlurController.captureAndBlur, cache trong
 *      BlurEngine.blurBitmap). Chỉ cần scale bitmap cache đó lên full-screen + tint - CHỈ
 *      dùng cache, KHÔNG tự chụp/blur lại (chụp lại mỗi lần mở trang sẽ rất chậm + không
 *      đúng ý nghĩa "ảnh nền" tĩnh theo wallpaper).
 *
 *   2. MỞ DIALOG (getDialogBlurBackground): dialog che lên NỘI DUNG THẬT đang hiển thị của
 *      trang hiện tại (danh sách, text đang gõ dở, v.v...) - ảnh wallpaper cache ở trên
 *      không phản ánh đúng những gì đang thấy trên màn hình. Vì vậy dialog LUÔN chụp
 *      screenshot màn hình activity tại đúng thời điểm mở dialog rồi blur bằng RenderScript
 *      (BlurController) - KHÔNG dùng lại cache wallpaper, đảm bảo nền mờ phía sau dialog
 *      luôn khớp với nội dung thật đang hiển thị ngay trước đó.
 *
 * Cả 2 đều dùng chung RenderScript pipeline của BlurController (GPU-accelerated, ổn định)
 * thay vì StackBlur CPU cũ, và cùng áp dụng contrast thích ứng dark/light mode qua tint.
 *
 * clearCache() phải được gọi khi đổi theme (dark/light hoặc đổi kiểu nền) - ảnh cache cũ đã
 * bake sẵn contrast/tint theo theme CŨ, dùng tiếp sẽ bị sai màu/độ tương phản cho tới khi
 * capture mới xong; xoá cache buộc trang mới phải chờ/for hiện bitmap mới thay vì thấy nhầm
 * ảnh cũ.
 */
object FastBlurUtility {

    /**
     * Ảnh nền mờ cho TRANG MỚI (activity/fragment mới mở) - CHỈ lấy từ cache wallpaper đã
     * blur sẵn (BlurEngine.blurBitmap), KHÔNG tự chụp/blur lại.
     *
     * @return bitmap full-screen đã blur + tint, hoặc null nếu cache chưa sẵn sàng (ví dụ
     *         BlurController vẫn đang capture nền ở background thread - trang nên tạm hiện
     *         nền trống rồi tự cập nhật khi BlurEngine.blurBitmap có giá trị, xem
     *         BlurPreDrawListener). Caller phải recycle bitmap khi không còn dùng.
     */
    @JvmStatic
    fun getPageBlurBackground(activity: Activity): Bitmap? {
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val screenHeight = activity.resources.displayMetrics.heightPixels
        if (screenWidth <= 0 || screenHeight <= 0) return null

        val cachedBlur = BlurEngine.blurBitmap
        if (cachedBlur == null || cachedBlur.isRecycled) return null

        return scaleWithTint(cachedBlur, screenWidth, screenHeight)
    }

    /**
     * Ảnh nền mờ cho DIALOG - LUÔN chụp screenshot màn hình activity hiện tại rồi blur bằng
     * RenderScript, KHÔNG dùng cache wallpaper (BlurEngine.blurBitmap), để nền mờ phía sau
     * dialog khớp đúng nội dung thật đang hiển thị ngay trước khi dialog mở.
     *
     * @return bitmap full-screen đã blur + tint, hoặc null nếu chụp/blur thất bại.
     *         Caller phải recycle bitmap khi không còn dùng.
     */
    @JvmStatic
    fun getDialogBlurBackground(activity: Activity): Bitmap? {
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val screenHeight = activity.resources.displayMetrics.heightPixels
        if (screenWidth <= 0 || screenHeight <= 0) return null

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
     * Xoá cache blur wallpaper (BlurEngine.blurBitmap) - gọi khi đổi theme (dark/light,
     * hoặc bật/tắt directbg) để tránh getPageBlurBackground() trả về ảnh đã bake contrast/
     * tint theo theme CŨ trong lúc chờ BlurController capture xong bản mới.
     *
     * An toàn gọi nhiều lần / gọi khi cache đang null.
     */
    @JvmStatic
    fun clearCache() {
        val cached = BlurEngine.blurBitmap
        if (cached != null && !cached.isRecycled) {
            cached.recycle()
        }
        BlurEngine.blurBitmap = null
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
