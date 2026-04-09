package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BlurViewLinearLayout extends LinearLayout {
    protected BlurEngine engine;
    private RectF strokeRect = new RectF();
    private Rect srcRect = new Rect();
    private Rect dstRect = new Rect();

    public BlurViewLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new BlurEngine(this);
        // Quan trọng: Phải set false để hệ thống gọi hàm onDraw của ViewGroup
        setWillNotDraw(false); 
    }

    public BlurEngine getEngine() {
        return engine;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.engine.setup();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 1. Vẽ lớp kính mờ (Blur) ngay tại tọa độ hiện tại
        if (!BlurEngine.isPaused) {
            // Lấy bitmap đã được engine cắt và nhuộm màu sẵn
            Bitmap blurFragment = engine.getUpdatedBlurBitmap();
            
            if (blurFragment != null && !blurFragment.isRecycled()) {
                // Thiết lập vùng nguồn (toàn bộ bitmap đã cắt)
                srcRect.set(0, 0, blurFragment.getWidth(), blurFragment.getHeight());
                // Thiết lập vùng đích (khớp hoàn toàn với kích thước View hiện tại)
                dstRect.set(0, 0, getWidth(), getHeight());
                
                // Vẽ trực tiếp lên canvas trước khi vẽ các View con
                canvas.drawBitmap(blurFragment, srcRect, dstRect, null);
            }
        }

        // 2. Vẽ nội dung của View (super.onDraw sẽ vẽ TabLayout, chữ, icon...)
        super.onDraw(canvas);

        // 3. Vẽ viền (Stroke) lên trên cùng
        drawStroke(canvas);
    }

    protected void drawStroke(Canvas canvas) {
        // Sử dụng paint tĩnh từ engine để tối ưu
        android.graphics.Paint paint = BlurEngine.getStrokePaint();
        float radius = engine.cornerRadius;
        float strokeWidth = paint.getStrokeWidth();

        // Inset nửa độ dày viền để nét vẽ nằm trọn bên trong biên View
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
        // Dọn dẹp bộ đệm bitmap để tránh rò rỉ bộ nhớ
        if (engine != null) {
            engine.destroy();
        }
    }
}
