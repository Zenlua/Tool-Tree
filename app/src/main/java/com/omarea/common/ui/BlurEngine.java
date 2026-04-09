package com.omarea.common.ui;

import android.graphics.*;
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

    // Sử dụng Rect để tính toán tọa độ cắt ảnh
    private Rect srcRect = new Rect();
    // Bitmap đệm để tránh tạo mới liên tục
    private Bitmap cachedBitmap;
    private Canvas cachedCanvas;

    private int getBlurTintColor() {
        if (ThemeModeState.isDarkMode()) {
            return Color.parseColor("#88000000");
        } else {
            return Color.parseColor("#c0FFFFFF");
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
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this));
    }

    /**
     * Lấy Bitmap mờ đã được cập nhật tọa độ. 
     * Tối ưu hóa bằng cách không tạo mới Bitmap nếu kích thước không đổi.
     */
    public Bitmap getUpdatedBlurBitmap() {
        if (isPaused || blurBitmap == null || targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
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
            try {
                // KIỂM TRA: Nếu kích thước View thay đổi hoặc chưa có cache thì mới khởi tạo lại
                if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                    if (cachedBitmap != null) cachedBitmap.recycle();
                    cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    cachedCanvas = new Canvas(cachedBitmap);
                }

                // Cập nhật vùng cần cắt từ ảnh gốc
                srcRect.set(x, y, x + w, y + h);

                // Vẽ mảnh ảnh từ hình nền vào Bitmap đệm (Dùng Canvas để vẽ thay vì createBitmap liên tục)
                cachedCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); // Xóa cũ
                cachedCanvas.drawBitmap(blurBitmap, srcRect, new Rect(0, 0, w, h), null);
                
                // Phủ lớp màu theme lên trên
                cachedCanvas.drawColor(getBlurTintColor()); 
                
                return cachedBitmap;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static Paint getStrokePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5.0f); // Giảm nhẹ độ dày viền cho tinh tế
        
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
    
    // Đừng quên dọn dẹp khi View bị hủy
    public void destroy() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
    }
}
