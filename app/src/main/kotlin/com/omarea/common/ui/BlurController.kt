package com.omarea.common.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.tool.tree.ThemeModeState
import java.io.File
import java.lang.ref.WeakReference

class BlurController {

    // === Optimization 1: Cache RenderScript context + ScriptIntrinsicBlur ===
    // Tránh tạo mới RenderScript (rất nặng) mỗi lần blur.
    // RS context được tái sử dụng xuyên suốt lifecycle của controller (singleton).
    private var cachedRs: RenderScript? = null
    private var cachedBlurScript: ScriptIntrinsicBlur? = null
    private var cachedRsContext: Context? = null

    private fun getRenderScript(context: Context): Pair<RenderScript, ScriptIntrinsicBlur> {
        val rs = cachedRs
        val script = cachedBlurScript
        if (rs != null && !rs.isDestroyed && script != null && cachedRsContext === context) {
            return Pair(rs, script)
        }
        // Hủy RS cũ nếu context thay đổi
        rs?.destroy()
        val newRs = RenderScript.create(context)
        val newScript = ScriptIntrinsicBlur.create(newRs, Element.U8_4(newRs))
        cachedRs = newRs
        cachedBlurScript = newScript
        cachedRsContext = context
        return Pair(newRs, newScript)
    }

    /**
     * Hủy RenderScript cache (gọi khi không còn cần blur nữa).
     */
    fun destroyRenderScript() {
        cachedRs?.destroy()
        cachedRs = null
        cachedBlurScript = null
        cachedRsContext = null
    }

    // === Optimization 1 + 5: Cache RS + Allocation.destroy() ===
    private fun blurBitmap(context: Context, bitmap: Bitmap?, radius: Float): Bitmap? {
        if (bitmap == null) return null
        val outBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val (rs, intrinsicBlur) = getRenderScript(context)
        try {
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createFromBitmap(rs, outBitmap)
            intrinsicBlur.setRadius(radius)
            intrinsicBlur.setInput(input)
            intrinsicBlur.forEach(output)
            output.copyTo(outBitmap)
            // Optimization 5: Giải phóng Allocation ngay sau dùng để tránh rò rỉ native memory
            input.destroy()
            output.destroy()
        } catch (e: Exception) {
            outBitmap.recycle()
            return null
        }
        return outBitmap
    }

    /**
     * Chụp màu background solid (dùng khi directbg=1).
     * Tạo bitmap solid color → scale xuống 20% → blur.
     * Contrast được áp dụng ở giai đoạn vẽ (BlurEngine) thay vì tạo bitmap trung gian.
     */
    fun captureBackground(activity: Activity) {
        val activityRef = WeakReference(activity)

        Thread {
            val act = activityRef.get()
            if (act == null || act.isFinishing || act.isDestroyed) return@Thread

            val context = act.applicationContext
            val bgColor = BlurEngine.directBgColor

            // Optimization 4: Tăng scale từ 0.15 lên 0.20 để blur mượt hơn
            val screenWidth = act.resources.displayMetrics.widthPixels
            val screenHeight = act.resources.displayMetrics.heightPixels
            val width = Math.max(Math.round(screenWidth * 0.20f), 1)
            val height = Math.max(Math.round(screenHeight * 0.20f), 1)

            val solidBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(solidBitmap)
            canvas.drawColor(bgColor)

            // Optimization 2: Bỏ adjustContrast ở đây, contrast áp dụng ở BlurEngine khi vẽ
            val blurredResult = blurBitmap(context, solidBitmap, 16f)

            if (blurredResult != null) {
                // Cập nhật contrast value để BlurEngine áp dụng khi draw
                BlurEngine.blurContrast = if (ThemeModeState.isDarkMode()) 0.9f else 1.2f
                BlurEngine.blurBitmap = blurredResult
                BlurEngine.isPaused = false

                act.runOnUiThread {
                    if (!act.isFinishing && act.window != null) {
                        act.window.decorView.invalidate()
                    }
                }
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
            var isFromFile = false
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
                isFromFile = true
            } else {
                val wm = WallpaperManager.getInstance(context)
                wm.forgetLoadedWallpaper()
                val drawable = wm.drawable
                if (drawable is BitmapDrawable) {
                    source = drawable.bitmap
                }
            }

            // 2. Scale → Blur (Optimization 2: bỏ adjustContrast trung gian)
            if (source != null) {
                // Optimization 4: Tăng scale từ 0.15 lên 0.20 để blur mượt hơn
                val width = Math.max(Math.round(source.width * 0.20f), 1)
                val height = Math.max(Math.round(source.height * 0.20f), 1)
                val scaledSource = Bitmap.createScaledBitmap(source, width, height, false)

                // Giải phóng source nếu đọc từ file (BitmapFactory tạo bitmap mới)
                if (isFromFile) {
                    source.recycle()
                }

                val blurredResult = blurBitmap(context, scaledSource, 16f)

                // Giải phóng bitmap trung gian sau khi blur xong
                scaledSource.recycle()

                if (blurredResult != null) {
                    // Optimization 2: Lưu contrast value để BlurEngine áp dụng khi draw
                    BlurEngine.blurContrast = if (ThemeModeState.isDarkMode()) 0.9f else 1.2f
                    BlurEngine.blurBitmap = blurredResult
                    BlurEngine.isPaused = false

                    act.runOnUiThread {
                        if (!act.isFinishing && act.window != null) {
                            act.window.decorView.invalidate()
                        }
                    }
                }
            }
        }.start()
    }

    companion object {
        private var lastFileLength: Long = -1
        private var lastFileModified: Long = -1
    }
}
