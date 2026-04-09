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
        // 1. Vẽ lớp kính mờ (Blur) trước khi vẽ nội dung con
        if (!BlurEngine.isPaused) {
           engine.updateBlur();
        }

        // 2. Vẽ nội dung của View (Background mặc định, các View con như TabLayout, v.v.)
        super.onDraw(canvas);

        // 3. Vẽ viền (Stroke) lên trên cùng để không bị che khuất
        drawStroke(canvas);
    }

    protected void drawStroke(Canvas canvas) {
        float radius = engine.cornerRadius;
        float strokeWidth = BlurEngine.getStrokePaint().getStrokeWidth();

        // Thụt lề (Inset) để viền nằm trọn trong View, không bị cut mất 1 nửa
        float inset = strokeWidth / 2f;
        strokeRect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        if (radius > 0) {
            // Điều chỉnh bán kính bo góc nhẹ để khớp với đường thụt lề
            float adjustedRadius = Math.max(0, radius - inset);
            canvas.drawRoundRect(strokeRect, adjustedRadius, adjustedRadius, BlurEngine.getStrokePaint());
        } else {
            canvas.drawRect(strokeRect, BlurEngine.getStrokePaint());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Có thể thêm logic dọn dẹp bitmap trong engine tại đây nếu cần
    }
}
