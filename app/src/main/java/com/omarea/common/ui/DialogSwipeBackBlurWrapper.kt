package com.omarea.common.ui

import android.app.Activity
import android.graphics.Color
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
        
        // 1. Lấy layoutParams gốc của contentView và GIỮ NGUYÊN KHÔNG ĐỔI
        val originalLayoutParams = contentView.layoutParams

        // 2. Set window trong suốt để lộ activity phía sau khi vuốt
        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        
        parent.removeView(contentView)

        // 3. Tạo ImageView chứa ảnh blur phủ kín nền
        val blurImage = ImageView(activity).apply {
            setImageBitmap(blurBitmap)
            scaleType = ImageView.ScaleType.FIT_XY
        }

        // 4. Tạo wrapper, cấu hình focus để các ô EditText bên trong hoạt động bình thường, không bị kẹt bàn phím
        val wrapper = FrameLayout(activity).apply {
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Thêm ảnh blur làm lớp nền dưới cùng (MATCH_PARENT)
        wrapper.addView(
            blurImage, 
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        
        // Thêm lại contentView với CHÍNH XÁC originalLayoutParams gốc — không ép buộc hay tính toán lại gì cả
        wrapper.addView(contentView, originalLayoutParams)

        // 5. Đưa wrapper vào vị trí cũ của contentView với kích thước phủ kín màn hình
        val wrapperLp = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        parent.addView(wrapper, index, wrapperLp)

        return wrapper
    }
}
