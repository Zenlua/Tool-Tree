package com.omarea.krscript.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class TextBgImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var fillMode = false

    fun setScaleMode(mode: String) {
        when (mode.trim().lowercase()) {
            "crop", "cover", "fill" -> applyFill(ScaleType.CENTER_CROP)
            "stretch", "fit-xy" -> applyFill(ScaleType.FIT_XY)
            else -> {
                fillMode = false
                adjustViewBounds = true
                scaleType = ScaleType.FIT_CENTER
                requestLayout()
            }
        }
    }

    private fun applyFill(type: ScaleType) {
        fillMode = true
        adjustViewBounds = false
        scaleType = type
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (fillMode) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 0)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val host = parent as? View
        if (fillMode && host != null) {
            super.layout(0, 0, host.width, host.height)
        } else {
            super.layout(l, t, r, b)
        }
    }
}
