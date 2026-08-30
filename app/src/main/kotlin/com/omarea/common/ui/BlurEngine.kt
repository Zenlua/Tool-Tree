package com.omarea.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.view.View
import androidx.core.content.ContextCompat
import com.tool.tree.R
import com.tool.tree.ThemeModeState

class BlurEngine(private val targetView: View) {
    var cornerRadius: Float = DEFAULT_CORNER_RADIUS

    private val location = IntArray(2)
    private var cachedBitmap: Bitmap? = null
    private var cachedCanvas: Canvas? = null

    // ─── Cache BitmapShader ───────────────────────────────────────
    // Tái sử dụng shader khi blurBitmap không đổi, tránh tạo object mỗi frame
    // khi cuộn/vuốt (giảm GC pressure).
    private var cachedShader: BitmapShader? = null
    private var cachedShaderBitmap: Bitmap? = null

    // ─── Cache tint color ──────────────────────────────────────────
    // Chỉ đọc resource 1 lần, cache lại đến khi dark/light mode đổi.
    private var cachedTintColor: Int = 0
    private var cachedTintColorForDark: Boolean? = null

    // Tái sử dụng đối tượng để tránh tạo rác bộ nhớ (GC lag) khi vuốt/cuộn
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()

    fun setup() {
        if (cornerRadius > 0) {
            targetView.outlineProvider = BlurOutlineProvider(cornerRadius)
            targetView.clipToOutline = true
        } else {
            targetView.outlineProvider = null
            targetView.clipToOutline = false
        }
        targetView.viewTreeObserver.addOnPreDrawListener(BlurPreDrawListener(this, targetView))
    }

    fun getUpdatedBlurBitmap(): Bitmap? {
        val blurBitmap = BlurEngine.blurBitmap
        if (isPaused || blurBitmap == null || blurBitmap.isRecycled ||
            targetView.width <= 0 || targetView.height <= 0
        ) {
            return null
        }

        // Lấy RootView để tính toán tỷ lệ chính xác giữa màn hình và BlurBitmap
        val rootView = targetView.rootView
        if (rootView == null || rootView.width <= 0 || rootView.height <= 0) {
            return null
        }

        targetView.getLocationOnScreen(location)

        val scaleX = blurBitmap.width.toFloat() / rootView.width
        val scaleY = blurBitmap.height.toFloat() / rootView.height

        // Kích thước thực tế của vùng Blur trên View
        val w = (targetView.width * scaleX).toInt()
        val h = (targetView.height * scaleY).toInt()

        if (w <= 0 || h <= 0) return null

        // Tọa độ thực tế của View trên màn hình (có thể nhận giá trị âm khi vuốt ra ngoài biên)
        val x = (location[0] * scaleX).toInt()
        val y = (location[1] * scaleY).toInt()

        try {
            // Khởi tạo hoặc tái sử dụng cachedBitmap theo kích thước View
            var cached = cachedBitmap
            if (cached == null || cached.width != w || cached.height != h) {
                if (cached != null && !cached.isRecycled) {
                    cached.recycle()
                }
                cached = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                cachedBitmap = cached
                cachedCanvas = Canvas(cached)
                // Size đổi → shader cũ không dùng được nữa
                cachedShader = null
                cachedShaderBitmap = null
            }

            val canvas = cachedCanvas!!

            // Xóa canvas cũ
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)

            // Tái sử dụng BitmapShader nếu cùng source bitmap
            if (cachedShaderBitmap !== blurBitmap || cachedShader == null) {
                cachedShader = BitmapShader(blurBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                cachedShaderBitmap = blurBitmap
            }

            shaderMatrix.reset()
            shaderMatrix.postTranslate(-x.toFloat(), -y.toFloat())
            cachedShader!!.setLocalMatrix(shaderMatrix)

            shaderPaint.shader = cachedShader
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), shaderPaint)

            // Phủ lớp màu (Tint) lên trên lớp blur
            canvas.drawColor(getBlurTintColorCached())

            return cached
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Cache tint color — chỉ đọc resource khi dark/light mode thay đổi.
     * getBlurTintColor() gốc đọc ContextCompat.getColor() mỗi frame,
     * dù giá trị chỉ đổi khi chuyển theme.
     */
    private fun getBlurTintColorCached(): Int {
        val isDark = ThemeModeState.isDarkMode()
        if (cachedTintColorForDark != isDark) {
            cachedTintColorForDark = isDark
            val colorRes = if (isDark) R.color.colorBlurDark else R.color.colorBlurLight
            cachedTintColor = ContextCompat.getColor(targetView.context, colorRes)
        }
        return cachedTintColor
    }

    fun destroy() {
        val cached = cachedBitmap
        if (cached != null && !cached.isRecycled) {
            cached.recycle()
            cachedBitmap = null
        }
        cachedCanvas = null
        cachedShader = null
        cachedShaderBitmap = null
        cachedTintColorForDark = null
    }

    companion object {
        @JvmField
        var controller: BlurController = BlurController()

        @Volatile
        @JvmField
        var blurBitmap: Bitmap? = null

        @JvmField
        var isPaused = false

        /**
         * Khi directbg=1: không chụp wallpaper, chụp màu background solid thay thế.
         * Blur vẫn hoạt động bình thường (scale + RenderScript blur + tint)
         * nhưng nguồn ảnh là màu nền thay vì ảnh wallpaper.
         */
        @JvmField
        var isDirectBgMode = false

        @JvmField
        var DEFAULT_CORNER_RADIUS = 30.0f

        /**
         * Màu background hiện tại được dùng để tạo blur bitmap khi isDirectBgMode.
         * Cập nhật bởi ThemeModeState trước khi gọi capture.
         */
        @JvmField
        var directBgColor = 0xFF0f0f0f.toInt()

        private var strokePaint: Paint? = null

        @JvmStatic
        fun getStrokePaint(context: Context): Paint {
            var paint = strokePaint
            if (paint == null) {
                paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3.0f
                strokePaint = paint
            }
            val colorRes = if (ThemeModeState.isDarkMode()) R.color.colorPirmLight else R.color.colorPirmDark
            val color = ContextCompat.getColor(context, colorRes)
            if (paint.color != color) paint.color = color
            return paint
        }
    }
}
