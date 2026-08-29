package com.omarea.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout

open class BlurViewLinearLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    var engine: BlurEngine = BlurEngine(this)
    private val strokeRect = RectF()
    private val srcRect = Rect()
    private val dstRect = Rect()

    // MẶC ĐỊNH BẬT VẼ VIỀN CHO TẤT CẢ CÁC MÀN HÌNH
    private var drawStrokeEnabled = true

    init {
        setWillNotDraw(false)
    }

    // Hàm cho phép bật/tắt vẽ viền từ Code
    fun setDrawStrokeEnabled(enabled: Boolean) {
        this.drawStrokeEnabled = enabled
        invalidate() // Vẽ lại giao diện khi thay đổi
    }

    fun isDrawStrokeEnabled(): Boolean {
        return drawStrokeEnabled
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.engine.setup()
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Vẽ lớp kính mờ (Blur)
        if (!BlurEngine.isPaused) {
            val blurFragment = engine.getUpdatedBlurBitmap()

            if (blurFragment != null && !blurFragment.isRecycled) {
                srcRect.set(0, 0, blurFragment.width, blurFragment.height)
                dstRect.set(0, 0, width, height)
                canvas.drawBitmap(blurFragment, srcRect, dstRect, null)
            }
        }

        // 2. Vẽ nội dung giao diện con đè lên
        super.onDraw(canvas)

        // 3. CHỈ VẼ VIỀN NẾU ĐƯỢC CHO PHÉP (IF CHECK)
        if (drawStrokeEnabled) {
            drawStroke(canvas)
        }
    }

    protected open fun drawStroke(canvas: Canvas) {
        val paint = BlurEngine.getStrokePaint(context)
        val radius = engine.cornerRadius
        val strokeWidth = paint.strokeWidth

        val inset = strokeWidth / 2f
        strokeRect.set(inset, inset, width - inset, height - inset)

        if (radius > 0) {
            val adjustedRadius = Math.max(0f, radius - inset)
            canvas.drawRoundRect(strokeRect, adjustedRadius, adjustedRadius, paint)
        } else {
            canvas.drawRect(strokeRect, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        engine.destroy()
    }
}
