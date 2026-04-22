package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class BlurViewLinearLayout extends LinearLayout {
    protected BlurEngine engine;
    private android.graphics.RectF strokeRect = new android.graphics.RectF();
    private Rect srcRect = new Rect();
    private Rect dstRect = new Rect();

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
        if (!BlurEngine.isPaused) {
            // Lấy trực tiếp bitmap mờ từ Engine (đã bao gồm capture & blur)
            Bitmap blurFragment = engine.getUpdatedBlurBitmap();
            
            if (blurFragment != null && !blurFragment.isRecycled()) {
                srcRect.set(0, 0, blurFragment.getWidth(), blurFragment.getHeight());
                dstRect.set(0, 0, getWidth(), getHeight());
                canvas.drawBitmap(blurFragment, srcRect, dstRect, null);
            }
        }

        super.onDraw(canvas);
        drawStroke(canvas);
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
        if (engine != null) engine.destroy();
    }
}
