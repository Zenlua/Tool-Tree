package com.omarea.common.ui;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Lớp này giúp bo góc cho View và hỗ trợ đổ bóng (nếu cần).
 * Nó đảm bảo lớp kính mờ không bị lem ra ngoài các góc đã bo.
 */
public class BlurOutlineProvider extends ViewOutlineProvider {
    private float radius;

    public BlurOutlineProvider(float radius) {
        this.radius = radius;
    }

    @Override
    public void getOutline(View view, Outline outline) {
        // Kiểm tra kích thước để tránh lỗi trên một số dòng máy SDK 23-26
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            // Thiết lập vùng bao quanh là một hình chữ nhật bo góc (RoundRect)
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }
}
