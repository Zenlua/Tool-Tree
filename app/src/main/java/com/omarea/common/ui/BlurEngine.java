package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    
    // volatile để đảm bảo tính nhất quán dữ liệu giữa Thread của Controller và UI Thread
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
        // Kiểm tra an toàn Bitmap trước khi xử lý
        if (isPaused || blurBitmap == null || blurBitmap.isRecycled() || 
            targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        targetView.getLocationInWindow(location);
        
        View rootView = targetView.getRootView();
        // Tính toán tỷ lệ dựa trên kích thước thực tế của bitmap (đã được scale nhỏ ở Controller)
        float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
        float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();

        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);
        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);

        // Giới hạn vùng cắt bên trong phạm vi Bitmap
        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            try {
                if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                    // Giải phóng cached cũ nếu kích thước thay đổi
                    if (cachedBitmap != null) cachedBitmap.recycle();
                    cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    cachedCanvas = new Canvas(cachedBitmap);
                }
            
                srcRect.set(x, y, x + w, y + h);
                cachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR); 
                
                cachedCanvas.drawBitmap(blurBitmap, srcRect, new Rect(0, 0, w, h), null);
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
