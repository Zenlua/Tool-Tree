package com.omarea.common.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
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

        // Overlay mờ
        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#20ffffff"));

        // Lấy config
        ThemeConfig config = new ThemeConfig(context);
        blurEnabled = config.getAllowTransparentUI();

        // ❗ Tắt blur nếu Android < 12
        if (Build.VERSION.SDK_INT < 31) {
            blurEnabled = false;
        }

        // 🔥 Bo góc toàn bộ view (QUAN TRỌNG)
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight(),
                        cornerRadius
                );
            }
        });
        setClipToOutline(true);

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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyBlur();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Overlay mờ + bo góc
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

        if (Build.VERSION.SDK_INT < 31) {
            blurEnabled = false;
        }

        applyBlur();
        invalidate();
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
        applyBlur();
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;

        // cập nhật lại outline
        invalidateOutline();
        invalidate();
    }

    public void setOverlayColor(int color) {
        overlayPaint.setColor(color);
        invalidate();
    }
}