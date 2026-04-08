package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class BlurTopBarLayout extends BlurViewLinearLayout {
    public BlurTopBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Tắt bo góc cho thanh trên
        this.engine.cornerRadius = 0f;
    }

    @Override
    protected void drawStroke(Canvas canvas) {
        // Chỉ vẽ một đường kẻ ngang ở dưới cùng
        float strokeWidth = BlurEngine.getStrokePaint().getStrokeWidth();
        canvas.drawLine(0, getHeight() - strokeWidth/2, 
                        getWidth(), getHeight() - strokeWidth/2, 
                        BlurEngine.getStrokePaint());
    }
}
