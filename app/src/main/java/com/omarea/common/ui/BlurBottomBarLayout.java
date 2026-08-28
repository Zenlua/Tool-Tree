package com.omarea.common.ui;

import android.content.Context;
import android.util.AttributeSet;

public class BlurBottomBarLayout extends BlurViewLinearLayout {
    public BlurBottomBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Giao diện "đảo nổi": giữ bo góc mặc định thay vì tắt về 0, để thanh dưới trông
        // như một khối nổi tách biệt khỏi cạnh màn hình (kết hợp với margin ở layout XML).
        // Không override drawStroke() nữa: dùng viền bo tròn đầy đủ 4 cạnh của lớp cha
        // thay vì chỉ vẽ 1 đường kẻ ở cạnh trên.
    }
}
