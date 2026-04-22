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
        // Chỉ vẽ lại khi View đang hiển thị trên màn hình (tránh lãng phí tài nguyên cho các phần bị khuất trong ScrollView)
        if (targetView.isShown() && !BlurEngine.isPaused && BlurEngine.blurBitmap != null) {
            targetView.invalidate();
        }
        return true;
    }
}
