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
// của mục (checkbox/spinner/mở trang/script), KHÔNG áp dụng cho icon trái (icon trái luôn tuỳ
// chỉnh riêng theo icon-path của từng mục, xem ActionPage.buildPopupRow()).
// - CHECKBOX: mục type = "checkbox" -> hiện dấu tích/bỏ tích (checkbox_true/checkbox_false).
// - OPEN_PAGE: mục "mở trang" (link/activity/onlineHtmlPage/pageConfigSh/pageConfigPath không
//   rỗng) -> hiện mũi tên ">" báo hiệu bấm vào sẽ điều hướng sang trang khác.
// - DROPDOWN: mục type = "spinner" -> hiện mũi tên dropdown, thay cho hậu tố " ▾" gắn vào tiêu
//   đề như cách cũ (ActionPage.displayTitle() đã bị bỏ).
// - SCRIPT: các type còn lại (run/action/refresh/reload/restart/exit/file/folder...) - tức mục
//   bấm vào là chạy 1 script/hành động - hiện icon "script" mặc định (ic_editor_run) bên PHẢI.
enum class PopupRowRightIcon { SCRIPT, CHECKBOX, OPEN_PAGE, DROPDOWN }

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
            // GONE (không phải INVISIBLE) để tiêu đề canh sát lề trái khi mục không khai báo
            // icon riêng, thay vì để trống 1 khoảng chỗ icon như trước.
            iconLeft.visibility = View.GONE
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
            PopupRowRightIcon.SCRIPT -> {
                iconRight.visibility = View.VISIBLE
                iconRight.setImageResource(R.drawable.ic_editor_run)
            }
        }

        // Đường kẻ ngăn cách giữa các mục - ẩn ở dòng CUỐI CÙNG (không cần kẻ dưới mục cuối).
        view.findViewById<View>(R.id.popup_item_divider).visibility =
            if (position == rows.size - 1) View.GONE else View.VISIBLE

        return view
    }
}
