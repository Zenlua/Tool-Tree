package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.tool.tree.R;

public class BlurViewLinearLayout extends LinearLayout {
    protected BlurEngine engine;
    private RectF strokeRect = new RectF();
    private Rect srcRect = new Rect();
    private Rect dstRect = new Rect();

    // MẶC ĐỊNH BẬT VẼ VIỀN CHO TẤT CẢ CÁC MÀN HÌNH
    private boolean drawStrokeEnabled = true;

    public BlurViewLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new BlurEngine(this);
        setWillNotDraw(false);
    }

    // Hàm cho phép bật/tắt vẽ viền từ Code
    public void setDrawStrokeEnabled(boolean enabled) {
        this.drawStrokeEnabled = enabled;
        invalidate(); // Vẽ lại giao diện khi thay đổi
    }

    public boolean isDrawStrokeEnabled() {
        return drawStrokeEnabled;
    }

    public BlurEngine getEngine() {
        return engine;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.engine.setup();
    }

    // Paint tĩnh dùng cho chế độ Direct Background (vẽ tint trực tiếp)
    private Paint directBgPaint;
    private int lastDirectBgColor = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Vẽ lớp nền
        if (BlurEngine.isDirectBgMode) {
            // Chế độ Direct Background: KHÔNG chụp ảnh nền,
            // chỉ vẽ lớp tint trực tiếp → ảnh nền wallpaper hiển thị qua panel trong suốt
            int tintColor = getDirectBgTintColor();
            if (directBgPaint == null) {
                directBgPaint = new Paint();
            }
            directBgPaint.setColor(tintColor);
            canvas.drawRect(0, 0, getWidth(), getHeight(), directBgPaint);
        } else if (!BlurEngine.isPaused) {
            // Chế độ Blur bình thường: vẽ bitmap blur + tint
            Bitmap bgFragment = engine.getUpdatedBlurBitmap();
            
            if (bgFragment != null && !bgFragment.isRecycled()) {
                srcRect.set(0, 0, bgFragment.getWidth(), bgFragment.getHeight());
                dstRect.set(0, 0, getWidth(), getHeight());
                canvas.drawBitmap(bgFragment, srcRect, dstRect, null);
            }
        }

        // 2. Vẽ nội dung giao diện con đè lên
        super.onDraw(canvas);

        // 3. CHỈ VẼ VIỀN NẾU ĐƯỢC CHO PHÉP (IF CHECK)
        if (drawStrokeEnabled) {
            drawStroke(canvas);
        }
    }

    private int getDirectBgTintColor() {
        int colorRes = com.tool.tree.ThemeModeState.isDarkMode() ? R.color.colorBlurDark : R.color.colorBlurLight;
        return ContextCompat.getColor(getContext(), colorRes);
    }

    protected void drawStroke(Canvas canvas) {
        android.graphics.Paint paint = BlurEngine.getStrokePaint(getContext());
        float radius = engine.cornerRadius;
        float strokeWidth = paint.getStrokeWidth();

        float inset = strokeWidth / 2f;
        strokeRect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        if (radius > 0) {
            float adjustedRadius = Math.max(0, radius - inset);
            canvas.drawRoundRect(strokeRect, adjustedRadius, adjustedRadius, paint);
        } else {
            canvas.drawRect(strokeRect, paint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (engine != null) {
            engine.destroy();
        }
    }
}
