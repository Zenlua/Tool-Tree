package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static volatile Bitmap blurBitmap; 
    public static boolean isPaused = true;
    
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];

    private Rect srcRect = new Rect();
    private Bitmap cachedBitmap;
    private Canvas cachedCanvas;
    private static Paint strokePaint;

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
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this, targetView));
    }

    public Bitmap getUpdatedBlurBitmap() {
        // 1. Kiểm tra an toàn: Bitmap phải tồn tại và View phải có kích thước
        if (isPaused || blurBitmap == null || blurBitmap.isRecycled() || 
            targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        // 2. Lấy vị trí CHÍNH XÁC của View trên màn hình ngay tại thời điểm vẽ
        targetView.getLocationOnScreen(location); 
        
        // 3. Lấy kích thước toàn màn hình (hoặc root view) để tính tỷ lệ
        View rootView = targetView.getRootView();
        int rootWidth = rootView.getWidth();
        int rootHeight = rootView.getHeight();

        if (rootWidth <= 0 || rootHeight <= 0) return null;

        // 4. Tính toán tỷ lệ giữa ảnh blur đã scale và màn hình thực tế
        float scaleX = (float) blurBitmap.getWidth() / rootWidth;
        float scaleY = (float) blurBitmap.getHeight() / rootHeight;

        // 5. Tính toán tọa độ vùng cắt trên ảnh blur
        // Cần dùng float để tránh sai số tích lũy khi vuốt nhanh, sau đó mới ép kiểu int
        int x = Math.round(location[0] * scaleX);
        int y = Math.round(location[1] * scaleY);
        int w = Math.round(targetView.getWidth() * scaleX);
        int h = Math.round(targetView.getHeight() * scaleY);

        // 6. Ràng buộc tọa độ để không cắt ra ngoài phạm vi bitmap
        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            try {
                // Tối ưu bộ nhớ: Chỉ tạo lại bitmap đệm khi kích thước view thay đổi
                if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                    if (cachedBitmap != null) cachedBitmap.recycle();
                    cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    cachedCanvas = new Canvas(cachedBitmap);
                }
            
                srcRect.set(x, y, x + w, y + h);
                cachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR); 
                
                // Vẽ vùng ảnh tương ứng từ ảnh nền vào cache
                cachedCanvas.drawBitmap(blurBitmap, srcRect, new Rect(0, 0, w, h), null);
                
                // Nhuộm màu (Tint) để tạo hiệu ứng kính mờ (Frost Glass)
                cachedCanvas.drawColor(getBlurTintColor()); 
                
                return cachedBitmap;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private int getBlurTintColor() {
        Context context = targetView.getContext();
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorBlurDark : R.color.colorBlurLight;
        return ContextCompat.getColor(context, colorRes);
    }

    public static Paint getStrokePaint(Context context) {
        if (strokePaint == null) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3.0f); 
        }
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorPirmLight : R.color.colorPirmDark;
        int color = ContextCompat.getColor(context, colorRes);
        if (strokePaint.getColor() != color) {
            strokePaint.setColor(color);
        }
        return strokePaint;
    }

    public void destroy() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
        cachedCanvas = null;
    }
}
