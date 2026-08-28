package com.omarea.common.ui;

import android.content.Context;
import android.util.AttributeSet;

public class BlurTopBarLayout extends BlurViewLinearLayout {
    public BlurTopBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Giao diện "đảo nổi": giữ nguyên bo góc mặc định (BlurEngine.DEFAULT_CORNER_RADIUS)
        // thay vì ép về hình chữ nhật vuông cạnh như trước, để thanh top bar trông như một
        // khối nổi tách biệt khỏi mép màn hình (kết hợp với margin đặt ở layout XML).
        // Không override drawStroke() nữa: dùng viền bo tròn đầy đủ 4 cạnh của lớp cha
        // (BlurViewLinearLayout) thay vì chỉ vẽ 1 đường kẻ ở cạnh dưới, cho đúng cảm giác
        // "đảo" độc lập có viền bao quanh.
    }
}
