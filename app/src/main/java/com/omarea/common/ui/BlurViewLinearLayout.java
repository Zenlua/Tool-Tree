package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BlurViewLinearLayout extends LinearLayout {
    protected BlurEngine engine;
    private RectF strokeRect = new RectF(); // Khai báo dùng lại để tránh cấp phát bộ nhớ trong onDraw

    public BlurViewLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new BlurEngine(this);
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
        super.onDraw(canvas);
        drawStroke(canvas);
    }

    protected void drawStroke(Canvas canvas) {
        float radius = engine.cornerRadius;
        float strokeWidth = BlurEngine.getStrokePaint().getStrokeWidth();

        // GIẢI PHÁP: Thụt lề (Inset) vào một khoảng bằng nửa độ dày của viền
        // Điều này đảm bảo toàn bộ nét vẽ nằm bên trong giới hạn của View
        float inset = strokeWidth / 2f;
        
        strokeRect.set(
            inset, 
            inset, 
            getWidth() - inset, 
            getHeight() - inset
        );

        if (radius > 0) {
            // Điều chỉnh lại bán kính bo góc để khớp với vị trí mới sau khi thụt lề
            float adjustedRadius = Math.max(0, radius - inset);
            
            canvas.drawRoundRect(strokeRect, 
                adjustedRadius, adjustedRadius, 
                BlurEngine.getStrokePaint());
        } else {
            // Nếu không bo góc, vẽ hình chữ nhật thường nhưng vẫn phải thụt lề
            canvas.drawRect(strokeRect, BlurEngine.getStrokePaint());
        }
    }
}
