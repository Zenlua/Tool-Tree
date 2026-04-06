package com.omarea.common.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.tool.tree.ThemeConfig;

public class BlurViewLinearLayout extends LinearLayout {

    private float cornerRadius = 30f;
    private boolean blurEnabled = false;

    private Paint overlayPaint;

    // cache
    private Bitmap rootBitmap;
    private Bitmap blurBitmap;

    private int lastX = -1, lastY = -1;
    private int lastW = -1, lastH = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean pendingUpdate = false;

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
    }

    // ===== gọi thủ công khi cần =====
    public void updateBlur() {
        if (!blurEnabled) return;

        if (pendingUpdate) return;
        pendingUpdate = true;

        // delay nhẹ để tránh spam
        handler.postDelayed(() -> {
            pendingUpdate = false;
            doBlur();
        }, 120);
    }

    private void doBlur() {
        try {
            Context ctx = getContext();
            if (!(ctx instanceof Activity)) return;

            Activity act = (Activity) ctx;

            View root = act.findViewById(android.R.id.content);
            if (root.getWidth() == 0 || root.getHeight() == 0) return;

            // 🔥 cache root bitmap (không tạo lại liên tục)
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

            // ===== tính vị trí chuẩn =====
            int[] loc = new int[2];
            int[] rootLoc = new int[2];

            getLocationOnScreen(loc);
            root.getLocationOnScreen(rootLoc);

            int x = loc[0] - rootLoc[0];
            int y = loc[1] - rootLoc[1];
            int w = getWidth();
            int h = getHeight();

            // tránh update nếu không đổi
            if (x == lastX && y == lastY && w == lastW && h == lastH) {
                return;
            }

            lastX = x;
            lastY = y;
            lastW = w;
            lastH = h;

            float scale = root.getWidth() / 220f; // 🔥 tối ưu hiệu năng

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
        updateBlur();
    }
}