package com.omarea.common.ui

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable

object DialogSwipeBackBlurWrapper {
    fun wrap(activity: Activity, window: Window, contentView: View): View? {
        val parent = contentView.parent as? ViewGroup ?: return null

        val blurBitmap = if (DialogHelper.disableBlurBg) {
            null
        } else {
            FastBlurUtility.getBlurBackgroundDrawer(activity)
        } ?: return null

        val index = parent.indexOfChild(contentView)
        val originalLayoutParams = contentView.layoutParams

        val originalWidth = originalLayoutParams.width
        val originalHeight = originalLayoutParams.height

        // SỬA LỖI 1: Xử lý an toàn layout params gốc, giữ lại margin và tính chất căn lề
        val contentLp = when (originalLayoutParams) {
            is FrameLayout.LayoutParams -> FrameLayout.LayoutParams(originalWidth, originalHeight, originalLayoutParams.gravity).apply {
                setMargins(originalLayoutParams.leftMargin, originalLayoutParams.topMargin, originalLayoutParams.rightMargin, originalLayoutParams.bottomMargin)
            }
            else -> {
                val gravity = (originalLayoutParams as? ViewGroup.MarginLayoutParams)?.let {
                    Gravity.CENTER
                } ?: Gravity.CENTER
                
                FrameLayout.LayoutParams(originalWidth, originalHeight, gravity).apply {
                    if (originalLayoutParams is ViewGroup.MarginLayoutParams) {
                        setMargins(originalLayoutParams.leftMargin, originalLayoutParams.topMargin, originalLayoutParams.rightMargin, originalLayoutParams.bottomMargin)
                    }
                }
            }
        }

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        parent.removeView(contentView)

        val blurImage = ImageView(activity).apply {
            setImageBitmap(blurBitmap)
            scaleType = ImageView.ScaleType.FIT_XY
        }

        // SỬA LỖI 2: Cấu hình wrapper hỗ trợ focus tốt hơn để EditText không bị mất trạng thái nhập liệu
        val wrapper = FrameLayout(activity).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        wrapper.addView(blurImage, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        wrapper.addView(contentView, contentLp)

        val wrapperLp = when (originalLayoutParams) {
            is ViewGroup.MarginLayoutParams -> ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            else -> ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        parent.addView(wrapper, index, wrapperLp)

        return wrapper
    }
}
