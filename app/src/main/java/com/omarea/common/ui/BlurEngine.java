package com.omarea.common.ui;

import android.graphics.*;
import android.view.View;
import com.tool.tree.ThemeModeState;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;
import android.content.Context;
import android.util.DisplayMetrics;

public final class BlurEngine {
    public static BlurController controller = new BlurController();
    public static volatile Bitmap blurBitmap; 
    public static boolean isPaused = false; // Mặc định để false để có thể chạy ngay
    
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
        if (isPaused || blurBitmap == null || blurBitmap.isRecycled() || 
            targetView.getWidth() <= 0 || targetView.getHeight() <= 0) {
            return null;
        }

        // Lấy tọa độ tuyệt đối trên màn hình thay vì trong Window
        targetView.getLocationOnScreen(location);
        
        Context context = targetView.getContext();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        
        // Tính tỷ lệ dựa trên kích thước thực tế của màn hình (Screen Metrics)
        // Điều này đảm bảo ảnh blur khớp 1:1 với Wallpaper gốc phía sau
        float scaleX = (float) blurBitmap.getWidth() / dm.widthPixels;
        float scaleY = (float) blurBitmap.getHeight() / dm.heightPixels;

        int w = (int) (targetView.getWidth() * scaleX);
        int h = (int) (targetView.getHeight() * scaleY);
        int x = (int) (location[0] * scaleX);
        int y = (int) (location[1] * scaleY);

        // Giới hạn vùng cắt để không bị OutOfBounds
        x = Math.max(0, Math.min(x, blurBitmap.getWidth() - w));
        y = Math.max(0, Math.min(y, blurBitmap.getHeight() - h));

        if (w > 0 && h > 0) {
            try {
                // Chỉ khởi tạo lại cachedBitmap khi kích thước view thay đổi (rất quan trọng cho ScrollView)
                if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
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
