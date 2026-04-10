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
        // Chỉ ra lệnh vẽ lại khi có dữ liệu bitmap và không bị tạm dừng
        if (!BlurEngine.isPaused && BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) {
            targetView.invalidate();
        }
        return true;
    }
}
