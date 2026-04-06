package com.omarea.common.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
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

        // 🔥 bo góc toàn bộ
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);

        // fallback Android < 12
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

        // Android 12+
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

    // ===== Android < 12 dùng FastBlurUtility =====
    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (!blurEnabled) return true;
        if (Build.VERSION.SDK_INT >= 31) return true;

        try {
            Context ctx = getContext();
            if (!(ctx instanceof Activity)) return true;

            Activity act = (Activity) ctx;

            // 🔥 lấy blur full màn hình
            BitmapDrawable drawable = new BitmapDrawable(
                    getResources(),
                    FastBlurUtility.getBlurBackgroundDrawer(act)
            );

            setBackground(drawable);

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