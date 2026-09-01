package com.omarea.common.ui

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ListPopupWindow
import android.widget.ListView

/**
 * Tiện ích dùng chung cho các popup dạng Spinner bo góc (ListPopupWindow + nền bo góc
 * kr_spinner_popup_bg) trong toàn app:
 *   - Clip nội dung popup (listView) theo đúng outline bo góc, để hiệu ứng ripple khi bấm
 *     item KHÔNG tràn ra ngoài phần bo góc (mặc định selector/ripple của item vẽ full
 *     rectangle, không biết gì về bo góc của nền popup bên ngoài).
 *   - Thêm dải ngăn cách mảnh (1dp) giữa các item, dùng đúng màu divider mặc định của
 *     theme hệ thống hiện tại (android:attr/listDivider) - tự đổi theo sáng/tối.
 *
 * Dùng chung cho: ActionPage (menu "⋮", popup FAB, dropdown spinner), ParamsSingleSelect
 * (spinner trong form param dạng combobox), MainActivity (popup chọn theme).
 */
object SpinnerPopupHelper {

    /**
     * Gọi SAU popup.show() - ListPopupWindow chỉ tạo ra listView thật sự sau khi show().
     *
     * @param radiusPx bán kính bo góc, phải khớp với bán kính đang dùng ở nền popup
     *                 (thường lấy từ R.dimen.kr_spinner_popup_radius) để clip khớp viền.
     */
    @JvmStatic
    fun applyRoundedClip(popup: ListPopupWindow, radiusPx: Float) {
        val listView = popup.listView ?: return

        listView.clipToOutline = true
        listView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }

        applyDivider(listView)
    }

    private fun applyDivider(listView: ListView) {
        val context = listView.context
        val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.listDivider))
        val divider = attrs.getDrawable(0)
        attrs.recycle()

        listView.divider = divider
        listView.dividerHeight = (context.resources.displayMetrics.density * 1f).toInt().coerceAtLeast(1)
    }
}
