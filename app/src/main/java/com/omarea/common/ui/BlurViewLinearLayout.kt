package com.omarea.common.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

/**
 * Layout LinearLayout hỗ trợ hiệu ứng Blur (Kính mờ)
 */
class BlurViewLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val blurHelper = BlurViewHelper(this)
    private var isSetupDone = false

    // Khởi tạo helper khi View được gắn vào Window
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupIfNeeded()
    }

    // Khởi tạo helper khi có View con được thêm vào
    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        setupIfNeeded()
    }

    private fun setupIfNeeded() {
        if (!isSetupDone) {
            blurHelper.init()
            isSetupDone = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Bạn có thể bổ sung vẽ Border (viền) tại đây nếu muốn giống bản gốc
    }

    // Cập nhật lại hiệu ứng khi người dùng cuộn nội dung
    override fun computeScroll() {
        super.computeScroll()
        blurHelper.updateBlurFrame()
    }

    // Cập nhật lại hiệu ứng khi thay đổi kích thước Layout
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        blurHelper.updateBlurFrame()
    }
}
