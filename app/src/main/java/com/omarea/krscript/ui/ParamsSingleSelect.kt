package com.omarea.krscript.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import com.tool.tree.ThemeModeState

class ParamsSingleSelect(
        private var actionParamInfo: ActionParamInfo,
        private var context: FragmentActivity,
        // Được gọi mỗi khi người dùng thay đổi lựa chọn (dùng cho cơ chế depend-on)
        private val onValueChanged: (() -> Unit)? = null
) {

    private val darkMode: Boolean = ThemeModeState.isDarkMode()
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options)

    // Thời gian click gần nhất để chống nhấp nhanh / mở trùng Popup
    private var lastOpenTime: Long = 0

    // Đọc giá trị hiện tại đang được chọn
    fun getValue(): String {
        return if (selectedIndex in options.indices) options[selectedIndex].value ?: "" else ""
    }

    private fun updateValueView(valueView: TextView, textView: TextView) {
        if (selectedIndex in options.indices) {
            valueView.text = options[selectedIndex].value
            textView.text = options[selectedIndex].title
        } else {
            valueView.text = ""
            textView.text = ""
            // Hiển thị hint khi chưa chọn hoặc danh sách rỗng
            textView.hint = context.getString(
                if (options.isEmpty()) R.string.picker_not_item else R.string.kr_please_select
            )
        }
    }

    fun render(): View {
        val allowNoSelection = actionParamInfo.allowNoSelection
        if (!allowNoSelection && selectedIndex !in options.indices && options.isNotEmpty()) {
            selectedIndex = 0
        }

        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null)
        val textView = layout.findViewById<TextView>(R.id.kr_param_spinner)
        val valueView = layout.findViewById<TextView>(R.id.kr_param_value).apply {
            tag = actionParamInfo.name
        }

        updateValueView(valueView, textView)

        val isEnabled = !actionParamInfo.readonly && options.isNotEmpty()
        layout.isEnabled = isEnabled
        textView.isEnabled = isEnabled

        if (isEnabled) {
            val clickListener = View.OnClickListener {
                if (options.size > 6) {
                    openSingleSelectDialog(valueView, textView)
                } else {
                    openListPopupWindow(layout, valueView, textView)
                }
            }
            layout.setOnClickListener(clickListener)
            textView.setOnClickListener(clickListener)
        }

        return layout
    }

    private fun openListPopupWindow(anchorView: View, valueView: TextView, textView: TextView) {
        // Chống nhấp nhanh
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 400) {
            return
        }
        lastOpenTime = currentTime

        val listPopupWindow = ListPopupWindow(context).apply {
            this.anchorView = anchorView
            isModal = true // Chạm bên ngoài sẽ tự đóng Popup
        }

        // Tạo danh sách tiêu đề hiển thị
        val displayTitles = options.map { it.title ?: "" }
        val adapter = ArrayAdapter(context, R.layout.kr_spinner_dropdown, R.id.text, displayTitles)

        listPopupWindow.setAdapter(adapter)

        listPopupWindow.setOnItemClickListener { _, _, position, _ ->
            if (selectedIndex != position) {
                selectedIndex = position
                updateValueView(valueView, textView)
                onValueChanged?.invoke()
            }
            listPopupWindow.dismiss()
        }

        listPopupWindow.show()
    }

    private fun openSingleSelectDialog(valueView: TextView, textView: TextView) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        DialogItemChooser(
            darkMode,
            ArrayList(options.mapIndexed { index, item ->
                SelectItem().apply {
                    title = item.title
                    selected = index == selectedIndex
                }
            }),
            false,
            object : DialogItemChooser.Callback {
                override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                    val newIndex = status.indexOf(true)
                    if (newIndex != selectedIndex) {
                        selectedIndex = newIndex
                        updateValueView(valueView, textView)
                        onValueChanged?.invoke()
                    }
                }
            }
        ).show(context.supportFragmentManager, "params-single-select")
    }
}
