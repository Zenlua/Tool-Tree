package com.omarea.common.ui;

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

    private int[] lastLoc = new int[]{-1, -1};
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

        // bo góc giống v31
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
            }
        });
        setClipToOutline(true);

        // giống v31: update trước khi draw
        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (!blurEnabled) return true;

        try {
            // 🔥 giống v31: dùng rootView
            View root = getRootView();
            if (root.getWidth() == 0 || root.getHeight() == 0) return true;

            // cache root bitmap
            if (rootBitmap == null ||
                    rootBitmap.getWidth() != root.getWidth() ||
                    rootBitmap.getHeight() != root.getHeight()) {

                if (rootBitmap != null) rootBitmap.recycle();

                rootBitmap = Bitmap.createBitmap(
                        root.getWidth(),
                        root.getHeight(),
                        Bitmap.Config.ARGB_8888
                );
            }

            Canvas rootCanvas = new Canvas(rootBitmap);
            root.draw(rootCanvas);

            // 🔥 vị trí theo window (giống v31)
            int[] loc = new int[2];
            getLocationInWindow(loc);

            int x = loc[0];
            int y = loc[1];
            int w = getWidth();
            int h = getHeight();

            // tránh update liên tục
            if (lastLoc[0] == x && lastLoc[1] == y && lastW == w && lastH == h) {
                return true;
            }

            lastLoc[0] = x;
            lastLoc[1] = y;
            lastW = w;
            lastH = h;

            // scale giống v31
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
                    Math.max(0, (int) (x / scale)),
                    Math.max(0, (int) (y / scale)),
                    Math.min(rootBitmap.getWidth(), (int) ((x + w) / scale)),
                    Math.min(rootBitmap.getHeight(), (int) ((y + h) / scale))
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

    public void refreshConfig() {
        ThemeConfig config = new ThemeConfig(getContext());
        blurEnabled = config.getAllowTransparentUI();
        invalidate();
    }
}