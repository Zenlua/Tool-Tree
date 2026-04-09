package com.omarea.common.ui;

import android.view.View;
import android.view.ViewTreeObserver;

public class BlurPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    private View targetView;
    private BlurEngine engine; // Thêm engine để đồng bộ logic

    // Cập nhật Constructor để nhận 2 tham số như BlurEngine đang gọi
    public BlurPreDrawListener(BlurEngine engine, View view) {
        this.engine = engine;
        this.targetView = view;
    }

    @Override
    public boolean onPreDraw() {
        // Chỉ invalidate khi không bị tạm dừng và có dữ liệu bitmap
        if (!BlurEngine.isPaused && BlurEngine.blurBitmap != null) {
            targetView.invalidate();
        }
        return true;
    }
}
