package com.tool.tree.ui

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.widget.LinearLayout

class BlurLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    30f,
                    30f,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }
}
