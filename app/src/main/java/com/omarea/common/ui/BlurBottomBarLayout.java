package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class BlurBottomBarLayout extends BlurViewLinearLayout {
    public BlurBottomBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Vẽ đường viền chỉ ở phía trên (Top Stroke)
        Paint paint = BlurEngine.getStrokePaint();
        float strokeWidth = paint.getStrokeWidth();

        // Vẽ từ (0, 0) đến (Width, 0)
        canvas.drawLine(0, strokeWidth/2, getWidth(), strokeWidth/2, paint);
    }
}
