package com.omarea.common.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.tool.tree.ThemeModeState
import java.io.File
import java.lang.ref.WeakReference

class BlurController {

    /**
     * Điều chỉnh độ tương phản (Contrast) của Bitmap
     * @param contrast 1.2f cho chế độ sáng, 0.7f cho chế độ tối
     */
    private fun adjustContrast(bitmap: Bitmap?, contrast: Float): Bitmap? {
        if (bitmap == null) return null

        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Công thức tính offset để giữ điểm xám trung tâm không bị lệch màu quá nhiều
        // offset = (1 - contrast) * 128
        val offset = (1f - contrast) * 128f

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return out
    }

    private fun blurBitmap(context: Context, bitmap: Bitmap?, radius: Float): Bitmap? {
        if (bitmap == null) return null
        val outBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val rs = RenderScript.create(context)
        try {
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createFromBitmap(rs, outBitmap)
            val intrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            intrinsicBlur.setRadius(radius)
            intrinsicBlur.setInput(input)
            intrinsicBlur.forEach(output)
            output.copyTo(outBitmap)
        } finally {
            rs.destroy()
        }
        return outBitmap
    }

    /**
     * Chụp màu background solid (dùng khi directbg=1).
     * Thay vì đọc wallpaper, tạo một bitmap solid color từ BlurEngine.directBgColor,
     * rồi đưa qua pipeline blur bình thường (scale + RenderScript blur + contrast).
     */
    fun captureBackground(activity: Activity) {
        val activityRef = WeakReference(activity)

        Thread {
            val act = activityRef.get()
            if (act == null || act.isFinishing || act.isDestroyed) return@Thread

            val context = act.applicationContext
            val bgColor = BlurEngine.directBgColor

            // Tạo bitmap solid color nhỏ (15% kích thước màn hình, giống logic wallpaper)
            val screenWidth = act.resources.displayMetrics.widthPixels
            val screenHeight = act.resources.displayMetrics.heightPixels
            val width = Math.max(Math.round(screenWidth * 0.15f), 1)
            val height = Math.max(Math.round(screenHeight * 0.15f), 1)

            val solidBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(solidBitmap)
            canvas.drawColor(bgColor)

            // Áp dụng contrast (giống wallpaper pipeline)
            val contrastValue: Float = if (ThemeModeState.isDarkMode()) {
                0.9f
            } else {
                1.2f
            }
            val processedSource = adjustContrast(solidBitmap, contrastValue)

            // Áp dụng blur (giống wallpaper pipeline)
            val blurredResult = blurBitmap(context, processedSource, 16f)

            if (blurredResult != null) {
                BlurEngine.blurBitmap = blurredResult
                BlurEngine.isPaused = false

                act.runOnUiThread {
                    if (!act.isFinishing && act.window != null) {
                        act.window.decorView.invalidate()
                    }
                }
            }

            // Dọn dẹp
            if (processedSource != null && processedSource != solidBitmap) {
                processedSource.recycle()
            }
            solidBitmap.recycle()
        }.start()
    }

    fun captureAndBlur(activity: Activity) {
        val activityRef = WeakReference(activity)

        Thread {
            val act = activityRef.get()
            if (act == null || act.isFinishing || act.isDestroyed) return@Thread

            var source: Bitmap? = null
            val context = act.applicationContext

            // 1. Lấy Wallpaper gốc
            val customWallpaperFile = File(act.filesDir, "home/etc/wallpaper.jpg")
            if (customWallpaperFile.exists()) {
                val currentLength = customWallpaperFile.length()
                val currentModified = customWallpaperFile.lastModified()

                if (currentLength == lastFileLength && currentModified == lastFileModified) {
                    val current = BlurEngine.blurBitmap
                    if (current != null && !current.isRecycled) return@Thread
                }

                lastFileLength = currentLength
                lastFileModified = currentModified
                source = BitmapFactory.decodeFile(customWallpaperFile.absolutePath)
            } else {
                val wm = WallpaperManager.getInstance(context)
                wm.forgetLoadedWallpaper()
                val drawable = wm.drawable
                if (drawable is BitmapDrawable) {
                    source = drawable.bitmap
                }
            }

            // 2. Xử lý logic Theme và Hiệu ứng
            if (source != null) {
                val contrastValue: Float = if (ThemeModeState.isDarkMode()) {
                    0.9f
                    // Chế độ tối: giảm tương phản, làm ảnh dịu đi
                } else {
                    1.2f
                    // Chế độ sáng: tăng tương phản, làm ảnh tươi sáng
                }

                // A. Áp dụng Contrast
                val processedSource = adjustContrast(source, contrastValue)
                val width = Math.max(Math.round(processedSource!!.width * 0.15f), 1)
                val height = Math.max(Math.round(processedSource.height * 0.15f), 1)
                val scaledSource = Bitmap.createScaledBitmap(processedSource, width, height, false)
                val blurredResult = blurBitmap(context, scaledSource, 16f)

                if (blurredResult != null) {
                    BlurEngine.blurBitmap = blurredResult
                    BlurEngine.isPaused = false

                    act.runOnUiThread {
                        if (!act.isFinishing && act.window != null) {
                            act.window.decorView.invalidate()
                        }
                    }
                }

                // Dọn dẹp bitmap trung gian để tránh rò rỉ bộ nhớ
                if (processedSource != source) {
                    processedSource.recycle()
                }
            }
        }.start()
    }

    companion object {
        private var lastFileLength: Long = -1
        private var lastFileModified: Long = -1
    }
}
