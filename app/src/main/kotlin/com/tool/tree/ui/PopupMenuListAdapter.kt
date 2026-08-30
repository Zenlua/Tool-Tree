package com.tool.tree.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.tool.tree.R

// Icon mặc định hiển thị bên PHẢI mỗi dòng trong popup List Item mới - quyết định theo "type"
// của mục (checkbox/spinner/mở trang), KHÔNG áp dụng cho icon trái (icon trái luôn tuỳ chỉnh
// riêng theo icon-path của từng mục, xem ActionPage.buildPopupRow()).
// - CHECKBOX: mục type = "checkbox" -> hiện dấu tích/bỏ tích (checkbox_true/checkbox_false).
// - OPEN_PAGE: mục "mở trang" (link/activity/onlineHtmlPage/pageConfigSh/pageConfigPath không
//   rỗng) -> hiện mũi tên ">" báo hiệu bấm vào sẽ điều hướng sang trang khác.
// - DROPDOWN: mục type = "spinner" -> hiện mũi tên dropdown, thay cho hậu tố " ▾" gắn vào tiêu
//   đề như cách cũ (ActionPage.displayTitle() đã bị bỏ).
// - NONE: các type còn lại (run/action/refresh/reload/restart/exit/file/folder...) -> KHÔNG có
//   icon phải mặc định, giữ gọn chỉ còn icon trái (nếu có) + tiêu đề, theo đúng yêu cầu.
enum class PopupRowRightIcon { NONE, CHECKBOX, OPEN_PAGE, DROPDOWN }

// 1 dòng dữ liệu cho popup kiểu List Item - dùng chung cho CẢ popup menu "⋮"
// (ActionPage.showOverflowMenuPopup()) LẪN popup chọn khi FAB có nhiều item
// (ActionPage.showFabChooser()) để đồng bộ giao diện.
class PopupMenuRow(
    val title: String,
    val leftIcon: Drawable?,
    val rightIcon: PopupRowRightIcon,
    val checked: Boolean,
    val onClick: () -> Unit
)

// Adapter cho ListPopupWindow - thay thế ArrayAdapter chỉ có chữ (kr_spinner_dropdown) trước
// đây bằng layout list item có icon trái/phải (popup_menu_list_item.xml). Vẫn dùng chung
// ListPopupWindow (giữ nguyên toàn bộ cơ chế neo góc/tự lật lên trên như cũ) - chỉ đổi phần
// adapter/nội dung hiển thị bên trong từng dòng, xem ActionPage.showListPopup().
class PopupMenuListAdapter(
    private val context: Context,
    private val rows: List<PopupMenuRow>
) : BaseAdapter() {
    override fun getCount(): Int = rows.size
    override fun getItem(position: Int): PopupMenuRow = rows[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.popup_menu_list_item, parent, false)
        val row = rows[position]

        view.findViewById<TextView>(R.id.popup_item_title).text = row.title

        val iconLeft = view.findViewById<ImageView>(R.id.popup_item_icon_left)
        if (row.leftIcon != null) {
            iconLeft.setImageDrawable(row.leftIcon)
            iconLeft.visibility = View.VISIBLE
        } else {
            // INVISIBLE (không phải GONE) để tiêu đề của tất cả các dòng trong popup luôn
            // thẳng hàng với nhau, kể cả khi chỉ 1 vài dòng có icon riêng.
            iconLeft.visibility = View.INVISIBLE
        }

        val iconRight = view.findViewById<ImageView>(R.id.popup_item_icon_right)
        when (row.rightIcon) {
            PopupRowRightIcon.CHECKBOX -> {
                iconRight.visibility = View.VISIBLE
                iconRight.setImageResource(if (row.checked) R.drawable.checkbox_true else R.drawable.checkbox_false)
            }
            PopupRowRightIcon.OPEN_PAGE -> {
                iconRight.visibility = View.VISIBLE
                iconRight.setImageResource(R.drawable.kr_arrow)
            }
            PopupRowRightIcon.DROPDOWN -> {
                iconRight.visibility = View.VISIBLE
                iconRight.setImageResource(R.drawable.ic_arrow_dropdown)
            }
            PopupRowRightIcon.NONE -> {
                // Không có gì - giữ gọn, chỉ icon trái + tiêu đề.
                iconRight.visibility = View.GONE
            }
        }

        return view
    }
}
