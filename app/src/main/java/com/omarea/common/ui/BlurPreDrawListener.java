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
        // CẬP NHẬT: 
        // Khi người dùng vuốt ViewPager, mặc dù targetView không thay đổi kích thước
        // nhưng vị trí của nó trên màn hình có thể thay đổi hoặc nội dung dưới nó thay đổi.
        if (!BlurEngine.isPaused && BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap.isRecycled()) {
            if (targetView.isShown()) { // Chỉ vẽ lại nếu View đang hiển thị
                targetView.invalidate();
            }
        }
        return true;
    }
}
