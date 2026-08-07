package com.omarea.krscript.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.omarea.common.model.SelectItem
import com.omarea.common.ui.DialogItemChooser
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import android.content.res.Configuration
import com.tool.tree.ThemeModeState

class ParamsMultipleSelect(
    private val actionParamInfo: ActionParamInfo,
    private val context: FragmentActivity,
    // Được gọi mỗi khi người dùng xác nhận thay đổi lựa chọn trong dialog con,
    // dùng để các param khác "depend-on" param này biết mà cập nhật ẩn/hiện.
    private val onValueChanged: (() -> Unit)? = null
) {
    private var options: ArrayList<SelectItem>? = null
    private var status = booleanArrayOf()
    private var labels: Array<String?> = arrayOf()
    private var values: Array<String?> = arrayOf()
    private val darkMode: Boolean = ThemeModeState.isDarkMode()

    // Thêm biến lưu mốc thời gian click mở để làm phương án dự phòng
    private var lastOpenTime: Long = 0

    // Đọc giá trị hiện tại (các mục đang được chọn), nối bằng separator của param.
    // Dùng cho cơ chế depend-on vì view trả về bởi render() là 1 layout tổng hợp,
    // không phải 1 View đơn (Spinner/EditText/...) nên không tự đọc được bằng cách thông thường.
    fun getValue(): String {
        val result = ArrayList<String?>()
        for (index in status.indices) {
            if (status[index]) {
                values.getOrNull(index)?.let { result.add(it) }
            }
        }
        return result.joinToString(actionParamInfo.separator)
    }

    fun render(): View {
        options = actionParamInfo.optionsFromShell
        options?.run {
            labels = map { it.title }.toTypedArray()
            values = map { it.value }.toTypedArray()
            status = ActionParamsLayoutRender.getParamOptionsSelectedStatus(actionParamInfo, this)
        }

        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_multiple_select, null)
        val textView = layout.findViewById<TextView>(R.id.kr_param_label_text)
        val valueView = layout.findViewById<TextView>(R.id.kr_param_value_text)
        val countView = layout.findViewById<TextView>(R.id.kr_param_count_text)

        valueView.tag = actionParamInfo.name

        setView(textView, valueView, countView)

        // Kiểm tra xem danh sách có trống hay không để vô hiệu hóa (làm mờ và chặn bấm)
        val isEmptyOptions = options.isNullOrEmpty()
        val enabled = !actionParamInfo.readonly && !isEmptyOptions

        textView.isEnabled = enabled
        textView.isClickable = enabled
        textView.isFocusable = enabled

        if (isEmptyOptions) {
            textView.text = null
            textView.hint = context.getString(R.string.picker_not_item)
        } else {
            textView.setOnClickListener {
                openDialog(textView, valueView, countView)
            }
        }

        return layout
    }

    private fun setView(textView: TextView, valueView: TextView, countView: TextView) {
        val resultValues = ArrayList<String?>()
        val resultLables = ArrayList<String?>()
        var count = 0
        for (index in status.indices) {
            if (status[index]) {
                values[index]?.run {
                    resultValues.add(this)
                }
                labels[index]?.run {
                    resultLables.add(this)
                }
                count++
            }
        }
        val resultValueStr = "" + resultValues.joinToString(actionParamInfo.separator)
        val resultLabelStr = if (resultLables.isNotEmpty()) "" + resultLables.joinToString("，") else ""

        // Giữ nguyên tính năng cũ: Nếu có lựa chọn thì gán text, nếu không có và danh sách trống thì hiện hint
        if (resultLabelStr.isNotEmpty()) {
            textView.text = resultLabelStr
            textView.hint = null
        } else {
            textView.text = ""
            if (options.isNullOrEmpty()) {
                textView.hint = context.getString(R.string.picker_not_item)
            }
        }
        
        valueView.text = resultValueStr
        countView.text = count.toString()
    }

    private fun openDialog(textView: TextView, valueView: TextView, countView: TextView) {
        val dialogTag = "params-multi-select"

        // [CHẶN CHIẾN LƯỢC 1]: Kiểm tra nếu Dialog này đã hiển thị trên màn hình thì không làm gì cả
        if (context.supportFragmentManager.findFragmentByTag(dialogTag) != null) {
            return
        }

        // [CHẶN CHIẾN LƯỢC 2]: Chặn click quá nhanh bằng thời gian hệ thống (phòng trường hợp FragmentManager chưa kịp cập nhật tag)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        options?.run {
            val items = ArrayList<SelectItem>()
            for (i in labels.indices) {
                items.add(SelectItem().apply {
                    title = "" + labels[i]
                    selected = status[i]
                })
            }
            // Dark/light mode đã được xử lý qua tham số `darkMode` truyền vào DialogItemChooser
            // (rồi xuống DialogFullScreen) - không cần xử lý thêm ở đây.
            DialogItemChooser(darkMode, ArrayList(items), true, object : DialogItemChooser.Callback {
                override fun onConfirm(selected: List<SelectItem>, result: BooleanArray) {
                    result.forEachIndexed { index, value ->
                        status[index] = value
                    }
                    setView(textView, valueView, countView)
                    onValueChanged?.invoke()
                }
            }).show(context.supportFragmentManager, dialogTag)
        }
    }
}
