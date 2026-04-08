package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BlurViewLinearLayout extends LinearLayout {
    private BlurEngine engine;

    public BlurViewLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.engine = new BlurEngine(this);
        setWillNotDraw(false); 
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.engine.setup();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Vẽ viền ngoài cho View
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), 
            BlurEngine.getCornerRadius(), BlurEngine.getCornerRadius(), 
            BlurEngine.getStrokePaint());
    }
}
