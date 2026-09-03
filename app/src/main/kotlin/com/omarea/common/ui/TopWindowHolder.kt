package com.omarea.common.ui

import android.view.Window
import java.lang.ref.WeakReference

/**
 * Theo dõi Window của các Dialog "full-screen, windowIsFloating=false" đang hiển thị
 * (ví dụ DialogLogFragment) -- loại Dialog này tự tách thành 1 lớp cửa sổ riêng, nằm TRÊN
 * cửa sổ gốc của Activity, KHÔNG giống AlertDialog/Dialog nổi bình thường (loại nổi vẫn nằm
 * chung nhóm sub-window với cửa sổ Activity nên không cần holder này).
 *
 * BannerNotificationManager mặc định gắn banner (sub-window TYPE_APPLICATION_ATTACHED_DIALOG)
 * vào token cửa sổ Activity gốc (CurrentActivityHolder) -- nếu đang có 1 dialog full-screen
 * kiểu trên che lên trên, banner gắn vào token Activity sẽ bị dialog đó che mất. Holder này
 * cho BannerNotificationManager biết cửa sổ nào MỚI THỰC SỰ đang ở trên cùng để gắn đúng token.
 *
 * Dùng ngăn xếp (stack) để hỗ trợ trường hợp nhiều dialog full-screen mở lồng nhau -- window ở
 * đỉnh stack (còn sống) luôn là cửa sổ trên cùng thực tế.
 *
 * Nơi gọi: DialogLogFragment.onStart() -> push(window), DialogLogFragment.onDestroyView() ->
 * pop(window).
 */
object TopWindowHolder {
    private val stack = ArrayDeque<WeakReference<Window>>()

    @Synchronized
    fun push(window: Window) {
        stack.removeAll { it.get() == null || it.get() === window }
        stack.addLast(WeakReference(window))
    }

    @Synchronized
    fun pop(window: Window) {
        stack.removeAll { it.get() == null || it.get() === window }
    }

    /** Cửa sổ trên cùng hiện tại (nếu có), tự lọc bỏ các tham chiếu đã bị giải phóng. */
    @Synchronized
    fun current(): Window? {
        while (stack.isNotEmpty()) {
            val window = stack.last().get()
            if (window == null) {
                stack.removeLast()
            } else {
                return window
            }
        }
        return null
    }
}
