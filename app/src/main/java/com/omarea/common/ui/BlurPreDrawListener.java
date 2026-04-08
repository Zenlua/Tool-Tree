package com.omarea.common.ui;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;

// Listener cập nhật vị trí
class BlurPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    private BlurEngine engine;
    public BlurPreDrawListener(BlurEngine e) { this.engine = e; }
    @Override public boolean onPreDraw() {
        engine.updateBlur();
        return true;
    }
}

// Bo góc cho View
class BlurOutlineProvider extends ViewOutlineProvider {
    private float radius;
    public BlurOutlineProvider(float r) { this.radius = r; }
    @Override public void getOutline(View v, Outline o) {
        o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
    }
}
