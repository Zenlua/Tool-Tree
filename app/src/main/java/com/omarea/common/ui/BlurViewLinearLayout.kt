package com.omarea.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class BlurViewLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val helper = BlurHelper(this)
    private var isInitialized = false

    init {
        // Cho phép ViewGroup gọi onDraw()
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInitialized) {
            helper.init()
            isInitialized = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Lấy ảnh đã cắt từ Helper
        val blur = helper.getCroppedBitmap()
        if (blur != null && !blur.isRecycled) {
            // 2. Vẽ ảnh mờ bao phủ toàn bộ diện tích View
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(blur, null, destRect, null)
        }
        
        // 3. Vẽ background XML (bo góc và ripple) đè lên trên ảnh mờ
        super.onDraw(canvas)
    }

    override fun computeScroll() {
        super.computeScroll()
        helper.update()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        helper.update()
    }
}
