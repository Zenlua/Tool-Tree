package com.omarea.common.ui;

import android.view.View;
import android.view.ViewTreeObserver;

public class BlurPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    private View targetView;
    private BlurEngine engine; 

    public BlurPreDrawListener(BlurEngine engine, View view) {
        this.engine = engine;
        this.targetView = view;
    }

    @Override
    public boolean onPreDraw() {
        // Kiểm tra hiển thị: Nếu View không hiển thị thì không cần phí tài nguyên vẽ
        if (targetView.isShown() && !BlurEngine.isPaused && BlurEngine.blurBitmap != null) {
            // Ép View vẽ lại liên tục để cập nhật vị trí Blur theo ScrollView
            targetView.invalidate();
        }
        return true;
    }
}
