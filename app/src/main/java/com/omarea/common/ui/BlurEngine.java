package com.omarea.common.ui;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; // Ảnh mờ tổng thể
    public static boolean isPaused = true;
    public static float cornerRadius = 30.0f;

    private View targetView;
    private int[] location = new int[2];

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
        targetView.setClipToOutline(true);
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this));
    }

    public void updateBlur() {
        if (blurBitmap == null || isPaused || targetView.getWidth() <= 0) return;

        targetView.getLocationInWindow(location);
        
        float scaleX = (float) blurBitmap.getWidth() / targetView.getRootView().getWidth();
        float scaleY = (float) blurBitmap.getHeight() / targetView.getRootView().getHeight();

        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            Bitmap cropped = Bitmap.createBitmap(blurBitmap, x, y, w, h);
            targetView.setBackground(new BitmapDrawable(targetView.getResources(), cropped));
        }
    }

    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4.0f);
        p.setColor(Color.parseColor("#30FFFFFF"));
        return p;
    }

    public static float getCornerRadius() { return cornerRadius; }
}
