package com.omarea.common.ui;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import com.tool.tree.ThemeModeState;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; 
    public static boolean isPaused = true;
    
    // Giữ giá trị mặc định cho toàn hệ thống
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    
    // Chuyển thành biến instance (không có static) để mỗi View có một radius riêng
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];

    private int getBlurTintColor() {
        if (ThemeModeState.isDarkMode()) {
            return Color.parseColor("#44000000");
        } else {
            return Color.parseColor("#c0FFFFFF");
        }
    }

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        // Kiểm tra nếu có bo góc thì mới tạo Outline, giúp tối ưu hiệu năng cho Top/Bottom Bar
        if (cornerRadius > 0) {
            targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
            targetView.setClipToOutline(true);
        } else {
            targetView.setOutlineProvider(null);
            targetView.setClipToOutline(false);
        }
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
            Bitmap cropped = Bitmap.createBitmap(blurBitmap, x, y, w, h);
            Canvas canvas = new Canvas(cropped);
            canvas.drawColor(getBlurTintColor()); 

            targetView.setBackground(new BitmapDrawable(targetView.getResources(), cropped));
        }
    }

    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4.0f);
        
        if (ThemeModeState.isDarkMode()) {
            p.setColor(Color.parseColor("#20FFFFFF"));
        } else {
            p.setColor(Color.parseColor("#20000000"));
        }
        return p;
    }

    // Không dùng static cho getter này nữa để lấy đúng giá trị của từng engine
    public float getCornerRadius() {
        return cornerRadius;
    }
}
