package com.omarea.krscript.ui

import android.content.res.Configuration
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListPopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import com.tool.tree.ThemeModeState

class ParamsSingleSelect(
        private var actionParamInfo: ActionParamInfo,
        private var context: FragmentActivity,
        // Được gọi mỗi khi người dùng thay đổi lựa chọn - áp dụng cho cả 2 kiểu hiển thị
        // (ListPopupWindow khi <=6 lựa chọn, dialog khi > 6 lựa chọn), dùng để các param khác
        // "depend-on" param này biết mà cập nhật ẩn/hiện.
        private val onValueChanged: (() -> Unit)? = null
) {

    private val darkMode: Boolean = ThemeModeState.isDarkMode()
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

    // Thêm biến lưu thời gian click lúc mở danh sách chọn (dùng chung cho cả dialog và
    // ListPopupWindow để tránh việc nhấn nhanh mở 2 danh sách chọn cùng lúc)
    private var lastOpenTime: Long = 0

    // Chỉ khác null khi render() dùng nhánh ListPopupWindow (<=6 lựa chọn) - đây là view
    // TextView đóng vai trò "ô hiển thị đang đóng" giống với Spinner trước đây, đồng thời là
    // anchor để neo ListPopupWindow khi mở ra.
    private var anchorView: TextView? = null

    // Đọc giá trị hiện tại đang được chọn (dùng cho cơ chế depend-on), áp dụng cho cả 2 kiểu
    // hiển thị (ListPopupWindow khi <=6 lựa chọn, dialog khi > 6 lựa chọn).
    fun getValue(): String {
        return if (selectedIndex > -1 && selectedIndex < options.size) options[selectedIndex].value ?: "" else ""
    }

    private fun updateValueView(valueView: TextView, textView: TextView) {
        if (selectedIndex > -1 && selectedIndex < options.size) {
            valueView.text = options[(selectedIndex)].value
            textView.text = options[(selectedIndex)].title
        } else {
            valueView.text = ""
            textView.text = ""
        }
    }

    // Cập nhật nội dung hiển thị của ô ListPopupWindow lúc ĐÓNG: hiện title của mục đang chọn,
    // hoặc hiện chữ gợi ý (hint) khi chưa có lựa chọn nào - giống hệt hành vi trước đây của
    // Spinner (mục placeholder rỗng dùng "hint" để vẽ chữ gợi ý bằng màu textColorHint).
    private fun updateAnchorText(anchor: TextView) {
        if (selectedIndex > -1 && selectedIndex < options.size) {
            anchor.text = options[selectedIndex].title
            anchor.hint = null
        } else {
            anchor.text = null
            // Phân biệt 2 tình huống khác nhau về ngữ nghĩa, giống hệt logic gốc:
            // - options rỗng: không có gì để chọn -> "Không có tùy chọn khả dụng"
            // - options có dữ liệu nhưng chưa khớp lựa chọn nào -> "Vui lòng chọn"
            anchor.hint = context.getString(
                    if (options.isEmpty()) R.string.picker_not_item else R.string.kr_please_select
            )
        }
    }

    fun render(): View {
        if (options.size > 6) {
            val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_single_select, null)
            val textView = layout.findViewById<TextView>(R.id.kr_param_single_select)
            val valueView = layout.findViewById<TextView>(R.id.kr_param_value).apply {
                tag = actionParamInfo.name
                updateValueView(this, textView)
            }
            textView.run {
                setOnClickListener {
                    openSingleSelectDialog(valueView, textView)
                }
            }

            return layout
        } else {
            val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null)

            // Chỉ bật tính năng "ô trống chưa chọn" khi action param khai báo rõ
            // allow-no-selection="true". Mặc định TẮT: nếu chưa có value/valueFromShell nào
            // khớp, tự chọn mục đầu tiên - đúng hành vi gốc (luôn cần có 1 giá trị hiệu lực để
            // hoạt động hiệu quả, tránh gây khó hiểu/lỗi khi getValue() trả về rỗng ngoài ý
            // muốn ở phần lớn trường hợp sử dụng).
            val allowNoSelection = actionParamInfo.allowNoSelection
            if (!allowNoSelection && (selectedIndex < 0 || selectedIndex >= options.size) && options.isNotEmpty()) {
                selectedIndex = 0
            }

            val anchor = layout.findViewById<TextView>(R.id.kr_param_spinner).apply {
                tag = actionParamInfo.name
            }
            anchorView = anchor
            updateAnchorText(anchor)

            val enabled = !actionParamInfo.readonly && options.isNotEmpty()
            anchor.isEnabled = enabled
            anchor.isClickable = enabled
            anchor.isFocusable = enabled

            if (enabled) {
                anchor.setOnClickListener {
                    openSingleSelectPopup(anchor)
                }
            }

            return layout
        }
    }

    // Thay thế cho Spinner dropdown trước đây: dùng ListPopupWindow neo (anchor) vào chính ô
    // đang hiển thị. Chiều rộng popup co giãn theo nội dung chữ (wrap_content) thay vì chiếm
    // hết bề ngang màn hình, và nền có inset 16dp trái/phải để không dính sát mép màn hình.
    private fun openSingleSelectPopup(anchor: TextView) {
        // Tránh việc nhấn nhanh mở 2 popup cùng lúc (dùng chung cơ chế chặn với dialog)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        val adapter = ArrayAdapter(context, R.layout.kr_spinner_dropdown, R.id.text, options)

        val popup = ListPopupWindow(context)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        // Mở rộng theo chữ (không chiếm hết chiều ngang màn hình) - nền popup được bọc
        // trong <inset> 16dp trái/phải (kr_spinner_popup_bg_light/dark) để khung luôn
        // cách mép màn hình 1 khoảng, không bị dính sát vào 2 bên.
        popup.width = ListPopupWindow.WRAP_CONTENT
        popup.setBackgroundDrawable(context.getDrawable(
                if (darkMode) R.drawable.kr_spinner_popup_bg_dark else R.drawable.kr_spinner_popup_bg_light
        ))
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            updateAnchorText(anchor)
            onValueChanged?.invoke()
            popup.dismiss()
        }
        popup.show()
        if (selectedIndex > -1 && selectedIndex < options.size) {
            popup.listView?.setSelection(selectedIndex)
        }
    }

    private fun openSingleSelectDialog(valueView: TextView, textView: TextView) {
        // >>> CHẶN TẠI ĐÂY: Tránh việc nhấn nhanh mở 2 DialogItemChooser cùng lúc
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        DialogItemChooser(darkMode, ArrayList(options.mapIndexed{index, item->
            SelectItem().apply {
                title = item.title
                selected = index == selectedIndex
            }
        }), false, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                // Không chặn nút xác nhận theo yêu cầu
                selectedIndex = status.indexOf(true)
                updateValueView(valueView, textView)
                onValueChanged?.invoke()
            }
        }).show(context.supportFragmentManager, "params-single-select")
    }
}
