package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;
import androidx.core.content.ContextCompat;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static Bitmap blurBitmap; 
    public static boolean isPaused = true;
    
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];

    private Rect srcRect = new Rect();
    private Bitmap cachedBitmap;
    private Canvas cachedCanvas;
    
    // Cache lại Paint để tránh cấp phát bộ nhớ trong onDraw
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
        
        // SỬA LỖI: Truyền cả engine và targetView vào listener
        targetView.getViewTreeObserver().addOnPreDrawListener(new BlurPreDrawListener(this, targetView));
    }

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
            // Trong file BlurEngine.java -> hàm getUpdatedBlurBitmap()
            try {
                // Kiểm tra thêm điều kiện isRecycled() của ảnh gốc
                if (blurBitmap == null || blurBitmap.isRecycled()) return null;
            
                if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                    // Thay vì recycle(), chỉ cần gán null để GC xử lý nếu muốn an toàn tuyệt đối
                    cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    cachedCanvas = new Canvas(cachedBitmap);
                }
            
                srcRect.set(x, y, x + w, y + h);
                cachedCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); 
                
                // Kiểm tra lại lần cuối trước khi vẽ
                if (!blurBitmap.isRecycled()) {
                    cachedCanvas.drawBitmap(blurBitmap, srcRect, new Rect(0, 0, w, h), null);
                    cachedCanvas.drawColor(getBlurTintColor()); 
                    return cachedBitmap;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private int getBlurTintColor() {
        Context context = targetView.getContext();
        // Kiểm tra Dark Mode từ trạng thái ứng dụng
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorBlurDark : R.color.colorBlurLight;
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * @param context Cần truyền context vào vì đây là phương thức static
     */
    public static Paint getStrokePaint(Context context) {
        if (strokePaint == null) {
            strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3.0f); 
        }
        
        // Xác định màu sắc dựa trên theme hiện tại
        int colorRes = ThemeModeState.isDarkMode() ? R.color.colorPirmLight : R.color.colorPirmDark;
        int color = ContextCompat.getColor(context, colorRes);
        
        // Chỉ cập nhật nếu màu sắc thực sự thay đổi để tối ưu hiệu năng vẽ
        if (strokePaint.getColor() != color) {
            strokePaint.setColor(color);
        }
        
        return strokePaint;
    }

    public float getCornerRadius() {
        return cornerRadius;
    }
    
    public void destroy() {
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
        cachedCanvas = null;
    }
}
