package com.omarea.common.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider

/**
 * Lớp này giúp bo góc cho View và hỗ trợ đổ bóng (nếu cần).
 * Nó đảm bảo lớp kính mờ không bị lem ra ngoài các góc đã bo.
 */
class BlurOutlineProvider(private var radius: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        // Kiểm tra kích thước để tránh lỗi trên một số dòng máy SDK 23-26
        if (view.width > 0 && view.height > 0) {
            // Thiết lập vùng bao quanh là một hình chữ nhật bo góc (RoundRect)
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
}
