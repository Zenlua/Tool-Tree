package com.tool.tree.ui

import android.view.View
import android.view.ViewGroup

/**
 * Tiện ích dùng chung để xoá trạng thái "pressed" (và hiệu ứng ripple/foreground đi kèm) còn sót
 * lại trên 1 cây view - dùng ở 2 nơi:
 *
 * 1. SwipeBackPreviewCache.capture() - để ảnh preview chụp lúc vuốt không dính hiệu ứng nhấn của
 *    item vừa bấm để mở trang.
 * 2. Activity.onRestart()/onResume() của MainActivity/ActionPage - để xoá NGAY LẬP TỨC hiệu ứng
 *    nhấn trên view CŨ đang hiển thị lại trong lúc dữ liệu trang đang được tải lại bất đồng bộ
 *    (loadPageConfig()/reloadTabs() chạy trên IO thread, có thể mất một khoảng thời gian trước
 *    khi view mới thay thế view cũ) - nếu không xoá ngay, người dùng sẽ thấy 1 khoảng "flash"
 *    hiện view cũ còn hiệu ứng nhấn trước khi view mới kịp thay vào.
 */
object PressStateUtils {

    /**
     * Xoá trạng thái "pressed" trên toàn bộ cây view và ép mọi drawable (background, foreground,
     * state list animator - kể cả hiệu ứng ripple) nhảy thẳng tới trạng thái cuối cùng ngay lập
     * tức, thay vì tự chạy animation rồi mới tắt.
     */
    fun clearPressedState(view: View) {
        try {
            if (view.isPressed) {
                view.isPressed = false
            }
            view.jumpDrawablesToCurrentState()
        } catch (_: Exception) {
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                clearPressedState(view.getChildAt(i))
            }
        }
    }
}
