package com.omarea.common.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.tool.tree.ThemeConfig;

public class BlurViewLinearLayout extends LinearLayout {

    private float cornerRadius = 30f;
    private boolean blurEnabled = false;

    private Paint overlayPaint;

    private Bitmap rootBitmap;
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

    private void init(Context context) {
        setWillNotDraw(false);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#20ffffff"));

        ThemeConfig config = new ThemeConfig(context);
        blurEnabled = config.getAllowTransparentUI();

        // bo góc
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);

        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (!blurEnabled) return true;

        try {
            Context ctx = getContext();
            if (!(ctx instanceof Activity)) return true;

            Activity act = (Activity) ctx;
            View root = act.getWindow().getDecorView();

            if (root.getWidth() == 0 || root.getHeight() == 0) return true;

            // 🔥 tạo bitmap root (chỉ khi cần)
            if (rootBitmap == null) {
                rootBitmap = Bitmap.createBitmap(
                        root.getWidth(),
                        root.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            }

            Canvas rootCanvas = new Canvas(rootBitmap);
            root.draw(rootCanvas);

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

            float scale = root.getWidth() / 180f;

            int bw = Math.max(1, (int) (w / scale));
            int bh = Math.max(1, (int) (h / scale));

            if (blurBitmap == null ||
                    blurBitmap.getWidth() != bw ||
                    blurBitmap.getHeight() != bh) {

                if (blurBitmap != null) blurBitmap.recycle();

                blurBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(blurBitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);

            Rect src = new Rect(
                    (int) (x / scale),
                    (int) (y / scale),
                    (int) ((x + w) / scale),
                    (int) ((y + h) / scale)
            );

            Rect dst = new Rect(0, 0, bw, bh);

            canvas.drawBitmap(rootBitmap, src, dst, null);

            setBackground(new BitmapDrawable(getResources(), blurBitmap));

        } catch (Exception ignored) {
        }

        return true;
    };

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
}