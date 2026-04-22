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
        // CẬP NHẬT: Không còn phụ thuộc vào BlurEngine.blurBitmap tĩnh
        if (targetView.isShown() && !BlurEngine.isPaused) {
            targetView.invalidate();
        }
        return true;
    }
}
