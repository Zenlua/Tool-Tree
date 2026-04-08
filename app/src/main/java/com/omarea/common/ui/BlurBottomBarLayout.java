package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class BlurBottomBarLayout extends BlurViewLinearLayout {
    public BlurBottomBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Tắt bo góc cho thanh dưới
        this.engine.cornerRadius = 0f;
    }

    @Override
    protected void drawStroke(Canvas canvas) {
        // Chỉ vẽ một đường kẻ ngang ở trên cùng
        float strokeWidth = BlurEngine.getStrokePaint().getStrokeWidth();
        canvas.drawLine(0, strokeWidth/2, 
                        getWidth(), strokeWidth/2, 
                        BlurEngine.getStrokePaint());
    }
}
