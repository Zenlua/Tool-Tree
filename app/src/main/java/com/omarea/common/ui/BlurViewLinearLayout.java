package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BlurViewLinearLayout extends LinearLayout {
    protected BlurEngine engine; // Đổi thành protected để lớp con truy cập trực tiếp

    public BlurViewLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new BlurEngine(this);
        setWillNotDraw(false); 
    }

    // Cho phép lớp con lấy engine để cấu hình lại
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
        super.onDraw(canvas);
        drawStroke(canvas);
    }

    // Tách riêng logic vẽ viền để lớp con dễ dàng ghi đè
    protected void drawStroke(Canvas canvas) {
        float radius = engine.cornerRadius;
        if (radius <= 0) return;
    
        Paint paint = BlurEngine.getStrokePaint();
        float halfStroke = paint.getStrokeWidth() / 2f;
    
        RectF rect = new RectF(
            halfStroke,
            halfStroke,
            getWidth() - halfStroke,
            getHeight() - halfStroke
        );
    
        float innerRadius = Math.max(0, radius - halfStroke);
        canvas.drawRoundRect(rect, innerRadius, innerRadius, paint);
    }
}
