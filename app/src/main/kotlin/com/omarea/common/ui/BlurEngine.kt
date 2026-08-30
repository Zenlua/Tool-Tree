package com.omarea.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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

    // Tái sử dụng đối tượng để tránh tạo rác bộ nhớ (GC lag) khi vuốt/cuộn
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()

    // === Optimization 3: Cache BitmapShader ===
    // Chỉ tạo lại BitmapShader khi source bitmap thay đổi (wallpaper/theme mới),
    // KHÔNG tạo lại mỗi frame khi chỉ thay đổi vị trí (cuộn/vuốt).
    private var cachedShader: BitmapShader? = null
    private var shaderSourceBitmap: Bitmap? = null

    // === Optimization 3: Cache tint color ===
    // Chỉ đọc lại từ resources khi dark mode thay đổi.
    private var cachedTint: Int = 0
    private var cachedTintIsDark: Boolean? = null

    // === Optimization 2: Cache contrast ColorMatrixColorFilter ===
    // Contrast áp dụng qua paint.colorFilter thay vì tạo bitmap trung gian.
    private var cachedContrastFilter: ColorMatrixColorFilter? = null
    private var cachedContrastValue: Float = Float.NaN

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
            }

            val canvas = cachedCanvas!!

            // Xóa canvas cũ
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)

            // === Optimization 3: Tái sử dụng BitmapShader khi source bitmap không đổi ===
            var shader = cachedShader
            if (shader == null || shaderSourceBitmap !== blurBitmap) {
                shader = BitmapShader(blurBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                cachedShader = shader
                shaderSourceBitmap = blurBitmap
            }

            // Cập nhật matrix vị trí MỖI FRAME (quan trọng để blur bám đúng khi cuộn)
            shaderMatrix.reset()
            shaderMatrix.postTranslate(-x.toFloat(), -y.toFloat())
            shader.setLocalMatrix(shaderMatrix)

            // === Optimization 2: Áp dụng contrast qua ColorMatrixColorFilter ===
            val contrast = BlurEngine.blurContrast
            var contrastFilter = cachedContrastFilter
            if (cachedContrastValue != contrast) {
                if (contrast != 1.0f) {
                    val offset = (1f - contrast) * 128f
                    val cm = ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, offset,
                            0f, contrast, 0f, 0f, offset,
                            0f, 0f, contrast, 0f, offset,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    contrastFilter = ColorMatrixColorFilter(cm)
                } else {
                    contrastFilter = null
                }
                cachedContrastFilter = contrastFilter
                cachedContrastValue = contrast
            }

            // Gắn shader + contrast filter vào paint
            shaderPaint.shader = shader
            shaderPaint.colorFilter = contrastFilter
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), shaderPaint)

            // === Optimization 3: Cache tint color ===
            // Phủ lớp màu (Tint) lên trên lớp blur
            val tint = getCachedTintColor()
            if (tint != 0) {
                canvas.drawColor(tint)
            }

            return cached
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Optimization 3: Chỉ đọc tint color từ resources khi dark mode thay đổi.
     * Tránh gọi ContextCompat.getColor() mỗi frame.
     */
    private fun getCachedTintColor(): Int {
        val isDark = ThemeModeState.isDarkMode()
        if (cachedTintIsDark != isDark) {
            cachedTintIsDark = isDark
            val colorRes = if (isDark) R.color.colorBlurDark else R.color.colorBlurLight
            cachedTint = ContextCompat.getColor(targetView.context, colorRes)
        }
        return cachedTint
    }

    fun destroy() {
        val cached = cachedBitmap
        if (cached != null && !cached.isRecycled) {
            cached.recycle()
            cachedBitmap = null
        }
        cachedCanvas = null
        // Xóa cache shader khi view bị detach
        cachedShader = null
        shaderSourceBitmap = null
        cachedContrastFilter = null
        cachedContrastValue = Float.NaN
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

        /**
         * Optimization 2: Giá trị contrast được áp dụng qua ColorMatrixColorFilter
         * tại giai đoạn vẽ (BlurEngine.getUpdatedBlurBitmap) thay vì tạo bitmap trung gian.
         * Cập nhật bởi BlurController sau khi capture xong.
         */
        @Volatile
        @JvmField
        var blurContrast: Float = 1.0f

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
