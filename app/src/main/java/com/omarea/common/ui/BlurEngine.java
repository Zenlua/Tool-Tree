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
    public static boolean isPaused = false;
    
    public static float DEFAULT_CORNER_RADIUS = 30.0f;
    public float cornerRadius = DEFAULT_CORNER_RADIUS;

    private View targetView;
    private int[] location = new int[2];
    private int[] parentLocation = new int[2];
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
    
        View rootView = targetView.getRootView();
        if (rootView == null) return null;
        
        targetView.getLocationOnScreen(location);
    
        int w = targetView.getWidth();
        int h = targetView.getHeight();
    
        try {
            if (cachedBitmap == null || cachedBitmap.getWidth() != w || cachedBitmap.getHeight() != h) {
                if (cachedBitmap != null) cachedBitmap.recycle();
                cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                cachedCanvas = new Canvas(cachedBitmap);
            }
    
            // 1. Tính toán tỉ lệ scale
            float scaleX = (float) blurBitmap.getWidth() / rootView.getWidth();
            float scaleY = (float) blurBitmap.getHeight() / rootView.getHeight();
    
            // 2. Sử dụng Matrix để dịch chuyển Bitmap thay vì dùng srcRect
            Matrix matrix = new Matrix();
            // Dịch chuyển ngược lại vị trí của View trên màn hình để khớp nội dung
            matrix.postTranslate(-location[0], -location[1]);
            // Scale để khớp với kích thước blurBitmap đã được thu nhỏ
            matrix.postScale(scaleX, scaleY);
    
            // 3. Tạo Shader với chế độ CLAMP (Kéo dãn pixel biên khi tràn)
            BitmapShader shader = new BitmapShader(blurBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            shader.setLocalMatrix(matrix);
    
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(shader);
    
            // 4. Vẽ
            cachedCanvas.drawColor(0, PorterDuff.Mode.CLEAR); 
            cachedCanvas.drawRect(0, 0, w, h, paint);
            
            // Vẽ thêm lớp màu Tint
            cachedCanvas.drawColor(getBlurTintColor()); 
            
            return cachedBitmap;
        } catch (Exception e) {
            return null;
        }
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
        if (strokePaint.getColor() != color) strokePaint.setColor(color);
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
