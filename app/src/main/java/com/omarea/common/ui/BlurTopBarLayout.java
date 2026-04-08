package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

public class BlurTopBarLayout extends BlurViewLinearLayout {
    public BlurTopBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Vẽ đường viền chỉ ở phía dưới (Bottom Stroke)
        Paint paint = BlurEngine.getStrokePaint();
        float strokeWidth = paint.getStrokeWidth();
        
        // Vẽ từ (0, Height) đến (Width, Height)
        canvas.drawLine(0, getHeight() - strokeWidth/2, getWidth(), getHeight() - strokeWidth/2, paint);
    }
}
