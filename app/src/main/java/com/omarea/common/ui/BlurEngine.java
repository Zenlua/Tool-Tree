package com.omarea.common.ui;

import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import com.tool.tree.ThemeModeState;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; 
    public static boolean isPaused = true;
    
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];

    // Cache lại bitmap cũ để tái sử dụng, tránh tạo mới liên tục gây lag
    private Bitmap cachedBitmap;

    private int getBlurTintColor() {
        if (ThemeModeState.isDarkMode()) {
            return Color.parseColor("#44000000"); // Dark: mờ tối
        } else {
            return Color.parseColor("#c0FFFFFF"); // Light: mờ trắng sáng
        }
    }

    public BlurEngine(View view) {
        this.targetView = view;
    }

    public void setup() {
        if (cornerRadius > 0) {
            targetView.setOutlineProvider(new BlurOutlineProvider(cornerRadius));
            targetView.setClipToOutline(true);
        } else {
            targetView.setOutlineProvider(null);
            targetView.setClipToOutline(false);
        }
        // Listener này sẽ gọi updateBlur() mỗi khi màn hình sắp vẽ
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this));
    }

    // HÀM QUAN TRỌNG: Cập nhật và trả về Bitmap đã cắt đúng tọa độ
    public Bitmap getUpdatedBlurBitmap() {
        if (isPaused || blurBitmap == null || targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        // Lấy tọa độ thực tế của View trên cửa sổ hiện tại
        targetView.getLocationInWindow(location);
        
        View rootView = targetView.getRootView();
        float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
        float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();

        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        // Ràng buộc tọa độ trong phạm vi của ảnh blur gốc
        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            try {
                // Giải phóng bitmap cũ trước khi tạo mới để tránh tràn bộ nhớ (OOM)
                if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                    cachedBitmap.recycle();
                }

                // Cắt mảnh ảnh từ hình nền đã blur
                cachedBitmap = Bitmap.createBitmap(blurBitmap, x, y, w, h);
                
                // Vẽ đè màu Tint lên mảnh ảnh đã cắt
                Canvas canvas = new Canvas(cachedBitmap);
                canvas.drawColor(getBlurTintColor()); 
                
                return cachedBitmap;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // Bỏ hàm updateBlur cũ dùng setBackground để tránh vòng lặp vô tận
    @Deprecated
    public void updateBlur() {
        // Không dùng nữa
    }

    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5.0f); // Độ dày viền nhỏ lại một chút cho thanh thoát
        
        if (ThemeModeState.isDarkMode()) {
            p.setColor(Color.parseColor("#25FFFFFF"));
        } else {
            p.setColor(Color.parseColor("#20000000"));
        }
        return p;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }
}
