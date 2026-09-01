package com.tool.tree.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.tool.tree.R

// Icon mặc định hiển thị bên TRÁI mỗi dòng trong popup List Item - quyết định theo "type"
// của mục (checkbox/spinner/mở trang/mở link/reset/script). Hiện bên TRÁI khi mục KHÔNG
// có icon-path tuỳ chỉnh riêng (leftIcon == null); nếu có leftIcon thì ưu tiên leftIcon.
// - CHECKBOX: mục type = "checkbox" -> hiện dấu tích/bỏ tích (checkbox_true/checkbox_false).
//   Có thể tùy chỉnh icon checkbox bằng cách sửa 2 file drawable:
//     res/drawable/checkbox_true.xml  (trạng thái đã chọn)
//     res/drawable/checkbox_false.xml (trạng thái chưa chọn)
// - PAGE: mục "mở trang nội bộ" -> icon kr_page.
// - LINK: mục "mở link/html/activity ngoài" -> icon kr_link.
// - REFRESH: mục refresh/reload/restart/exit/finish/close/killapp -> icon kr_refresh.
// - DROPDOWN: mục type = "spinner" -> icon kr_down.
// - FILE: mục type = "file" -> icon kr_file.
// - FOLDER: mục type = "folder" -> icon kr_folder.
// - SCRIPT: các type còn lại (run/action...) -> icon kr_script.
// - NONE: không hiện icon mặc định nào (dùng khi leftIcon đã có).
enum class PopupRowTypeIcon { SCRIPT, CHECKBOX, PAGE, LINK, REFRESH, DROPDOWN, FILE, FOLDER, NONE }

// 1 dòng dữ liệu cho popup kiểu List Item - dùng chung cho CẢ popup menu "⋮"
// (ActionPage.showOverflowMenuPopup()) LẪN popup chọn khi FAB có nhiều item
// (ActionPage.showFabChooser()) để đồng bộ giao diện.
class PopupMenuRow(
    val title: String,
    val leftIcon: Drawable?,
    val typeIcon: PopupRowTypeIcon,
    val checked: Boolean,
    val onClick: () -> Unit
)

// Adapter cho ListPopupWindow - thay thế ArrayAdapter chỉ có chữ (kr_spinner_dropdown) trước
// đây bằng layout list item có icon trái (popup_menu_list_item.xml). Icon bên phải đã bị bỏ.
// Vẫn dùng chung ListPopupWindow (giữ nguyên toàn bộ cơ chế neo góc/tự lật lên trên như cũ)
// - chỉ đổi adapter/nội dung hiển thị bên trong từng dòng.
class PopupMenuListAdapter(
    private val context: Context,
    private val rows: List<PopupMenuRow>
) : BaseAdapter() {
    // Màu tint cho icon menu (giống toolbar icon) - lazy init 1 lần
    private val defaultTint: ColorStateList? by lazy {
        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.toolbarIconTint))
        val tint = ta.getColorStateList(0)
        ta.recycle()
        tint
    }

    // Màu accent cho checkbox khi đã tích
    private val accentTint: ColorStateList? by lazy {
        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorAccent))
    }

    override fun getCount(): Int = rows.size
    override fun getItem(position: Int): PopupMenuRow = rows[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.popup_menu_list_item, parent, false)
        val row = rows[position]

        view.findViewById<TextView>(R.id.popup_item_title).text = row.title

        val iconLeft = view.findViewById<ImageView>(R.id.popup_item_icon_left)

        // Ưu tiên icon tuỳ chỉnh (leftIcon từ icon-path); nếu không có thì dùng icon mặc
        // định theo loại mục (typeIcon). Cả hai đều không có thì ẩn icon (GONE).
        if (row.leftIcon != null) {
            iconLeft.setImageDrawable(row.leftIcon)
            // Icon tuỳ chỉnh (icon-path) cũng ép tint như icon mặc định.
            // Riêng checkbox khi đã tích thì dùng màu accent.
            iconLeft.imageTintList =
                if (row.typeIcon == PopupRowTypeIcon.CHECKBOX && row.checked) accentTint
                else defaultTint
            iconLeft.visibility = View.VISIBLE
        } else {
            when (row.typeIcon) {
                PopupRowTypeIcon.CHECKBOX -> {
                    iconLeft.setImageResource(if (row.checked) R.drawable.checkbox_true else R.drawable.checkbox_false)
                    // Checkbox đã tích: dùng màu accent; chưa tích: dùng màu toolbar icon
                    iconLeft.imageTintList = if (row.checked) accentTint else defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.PAGE -> {
                    iconLeft.setImageResource(R.drawable.kr_page)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.LINK -> {
                    iconLeft.setImageResource(R.drawable.kr_link)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.REFRESH -> {
                    iconLeft.setImageResource(R.drawable.kr_refresh)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.DROPDOWN -> {
                    iconLeft.setImageResource(R.drawable.kr_down)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.FILE -> {
                    iconLeft.setImageResource(R.drawable.kr_file)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.FOLDER -> {
                    iconLeft.setImageResource(R.drawable.kr_folder)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.SCRIPT -> {
                    iconLeft.setImageResource(R.drawable.kr_script)
                    iconLeft.imageTintList = defaultTint
                    iconLeft.visibility = View.VISIBLE
                }
                PopupRowTypeIcon.NONE -> {
                    iconLeft.visibility = View.GONE
                }
            }
        }

        // // Đường kẻ ngăn cách giữa các mục - ẩn ở dòng CUỐI CÙNG.
        // view.findViewById<View>(R.id.popup_item_divider).visibility =
            // if (position == rows.size - 1) View.GONE else View.VISIBLE

        return view
    }
}
