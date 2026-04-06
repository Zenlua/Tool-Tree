package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.tool.tree.ThemeConfig;

public class BlurViewLinearLayout extends LinearLayout {

    private float blurRadius = 20f;
    private float cornerRadius = 30f;

    private boolean blurEnabled = false;

    private Paint overlayPaint;

    // ===== fallback bitmap =====
    private Bitmap blurBitmap;
    private int lastX = -1, lastY = -1;
    private int lastW = -1, lastH = -1;

    public BlurViewLinearLayout(Context context) {
        super(context);
        init(context);
    }

    public BlurViewLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BlurViewLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#20ffffff"));

        ThemeConfig config = new ThemeConfig(context);
        blurEnabled = config.getAllowTransparentUI();

        // 🔥 bo góc
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);

        // fallback listener (Android < 12)
        if (Build.VERSION.SDK_INT < 31) {
            getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        }

        applyBlur();
    }

    private void applyBlur() {
        if (!blurEnabled) {
            if (Build.VERSION.SDK_INT >= 31) {
                setRenderEffect(null);
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                    RenderEffect.createBlurEffect(
                            blurRadius,
                            blurRadius,
                            Shader.TileMode.CLAMP
                    )
            );
        }
    }

    // ===== fallback blur (Android < 12) =====
    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (!blurEnabled) return true;

        if (Build.VERSION.SDK_INT >= 31) return true;

        try {
            View root = getRootView();
            if (root.getWidth() == 0 || root.getHeight() == 0) return true;

            int[] loc = new int[2];
            getLocationInWindow(loc);

            int x = loc[0];
            int y = loc[1];
            int w = getWidth();
            int h = getHeight();

            // tránh update liên tục
            if (x == lastX && y == lastY && w == lastW && h == lastH) {
                return true;
            }

            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;

            float scale = root.getWidth() / 150f; // blur strength

            int bw = Math.max(1, (int) (w / scale));
            int bh = Math.max(1, (int) (h / scale));

            if (blurBitmap == null || blurBitmap.getWidth() != bw || blurBitmap.getHeight() != bh) {
                if (blurBitmap != null) blurBitmap.recycle();
                blurBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(blurBitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            Bitmap rootBitmap = Bitmap.createBitmap(
                    root.getWidth(),
                    root.getHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas rootCanvas = new Canvas(rootBitmap);
            root.draw(rootCanvas);

            Rect src = new Rect(
                    (int) (x / scale),
                    (int) (y / scale),
                    (int) ((x + w) / scale),
                    (int) ((y + h) / scale)
            );

            Rect dst = new Rect(0, 0, bw, bh);

            canvas.drawBitmap(rootBitmap, src, dst, null);

            rootBitmap.recycle();

            setBackground(new BitmapDrawable(getResources(), blurBitmap));

        } catch (Exception ignored) {
        }

        return true;
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyBlur();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (blurBitmap != null) {
            blurBitmap.recycle();
            blurBitmap = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (blurEnabled) {
            canvas.drawRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    cornerRadius,
                    cornerRadius,
                    overlayPaint
            );
        }
    }

    // ===== API =====

    public void refreshConfig() {
        ThemeConfig config = new ThemeConfig(getContext());
        blurEnabled = config.getAllowTransparentUI();

        applyBlur();
        invalidate();
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
        applyBlur();
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        invalidateOutline();
        invalidate();
    }

    public void setOverlayColor(int color) {
        overlayPaint.setColor(color);
        invalidate();
    }
}