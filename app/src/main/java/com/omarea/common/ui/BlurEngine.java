package com.omarea.common.ui;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import com.tool.tree.ThemeModeState;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; 
    public static boolean isPaused = true;
    public static float cornerRadius = 30.0f;

    private View targetView;
    private int[] location = new int[2];

    // Hàm lấy màu phủ động dựa trên Dark Mode
    private int getBlurTintColor() {
        if (ThemeModeState.isDarkMode()) {
            return Color.parseColor("#44000000");
        } else {
            return Color.parseColor("#a0FFFFFF");
        }
    }

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
        targetView.setClipToOutline(true);
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this));
    }

    public void updateBlur() {
        if (isPaused || blurBitmap == null || targetView.getWidth() <= 0) {
            return;
        }

        targetView.getLocationInWindow(location);
        
        View rootView = targetView.getRootView();
        float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
        float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();

        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            // Cắt ảnh mờ
            Bitmap cropped = Bitmap.createBitmap(blurBitmap, x, y, w, h);
            
            // ÁP DỤNG MÀU PHỦ ĐỘNG TẠI ĐÂY
            Canvas canvas = new Canvas(cropped);
            canvas.drawColor(getBlurTintColor()); 

            targetView.setBackground(new BitmapDrawable(targetView.getResources(), cropped));
        }
    }

    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3.0f);
        
        // Cập nhật cả màu viền cho đồng bộ
        if (ThemeModeState.isDarkMode()) {
            p.setColor(Color.parseColor("#20FFFFFF"));
        } else {
            p.setColor(Color.parseColor("#20000000"));
        }
        return p;
    }

    public static float getCornerRadius() {
        return cornerRadius;
    }
}
