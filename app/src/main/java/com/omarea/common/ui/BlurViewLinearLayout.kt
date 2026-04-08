package com.omarea.common.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout

class BlurViewLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val helper = BlurHelper(this)
    private var isInitialized = false

    init {
        // QUAN TRỌNG: Để onDraw luôn được gọi trong ViewGroup
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        // 1. Lấy tấm ảnh mờ từ Helper
        val blurBitmap = helper.getCroppedBitmap()
        
        if (blurBitmap != null && !blurBitmap.isRecycled) {
            // 2. Vẽ tấm ảnh mờ lấp đầy diện tích View trước khi vẽ background XML
            val destRect = Rect(0, 0, width, height)
            canvas.drawBitmap(blurBitmap, null, destRect, null)
        }
        
        // 3. Sau đó mới gọi super.onDraw để Android vẽ cái 
        // android:background="@drawable/krscript_item_ripple_inactive" đè lên
        super.onDraw(canvas)
    }

    // Các hàm khác giữ nguyên để tính toán tọa độ
    override fun computeScroll() {
        helper.update()
        invalidate() // Ép vẽ lại liên tục khi cuộn
        super.computeScroll()
    }

    override fun onAttachedToWindow() {
        if (!isInitialized) { helper.init(); isInitialized = true }
        super.onAttachedToWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        helper.update()
        super.onSizeChanged(w, h, oldw, oldh)
    }
}
