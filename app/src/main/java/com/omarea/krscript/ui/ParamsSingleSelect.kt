package com.omarea.krscript.ui

import android.content.res.Configuration
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
        // (Spinner khi <=6 lựa chọn, dialog khi > 6 lựa chọn), dùng để các param khác
        // "depend-on" param này biết mà cập nhật ẩn/hiện.
        private val onValueChanged: (() -> Unit)? = null
) {

    private val darkMode: Boolean = ThemeModeState.isDarkMode()
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

    // Thêm biến lưu thời gian click lúc mở danh sách chọn
    private var lastOpenTime: Long = 0

    // Chỉ khác null khi render() dùng nhánh Spinner (<=6 lựa chọn)
    private var spinnerView: Spinner? = null

    // Đọc giá trị hiện tại đang được chọn (dùng cho cơ chế depend-on), áp dụng cho cả 2 kiểu
    // hiển thị (Spinner khi <=6 lựa chọn, dialog khi > 6 lựa chọn).
    fun getValue(): String {
        spinnerView?.let { spinner ->
            return (spinner.selectedItem as? SelectItem)?.value ?: ""
        }
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

            // Chèn 1 mục placeholder RỖNG ở đầu danh sách khi chưa có lựa chọn nào khớp
            // (selectedIndex == -1), để Spinner hiển thị "chưa chọn gì" thay vì hành vi mặc
            // định của Android là tự sáng mục đầu tiên trong danh sách (gây hiểu lầm cho
            // người dùng là mục đó đang được chọn dù thực ra chưa hề chọn).
            val hasNoSelection = selectedIndex < 0 || selectedIndex >= options.size
            val displayOptions: List<SelectItem> = if (hasNoSelection) {
                ArrayList<SelectItem>(options.size + 1).apply {
                    add(SelectItem()) // title/value đều null -> SelectItem.toString() trả về rỗng
                    addAll(options)
                }
            } else {
                options
            }
            // Vì placeholder (nếu có) luôn nằm ở vị trí 0 và danh sách không đổi sau khi dựng,
            // độ lệch này giữ nguyên trong suốt vòng đời view - vị trí Spinner (pos) trừ đi
            // indexOffset luôn ra đúng index thật trong `options`.
            val indexOffset = if (hasNoSelection) 1 else 0

            layout.findViewById<Spinner>(R.id.kr_param_spinner).run {
                tag = actionParamInfo.name
                spinnerView = this

                // Adapter tuỳ chỉnh: mục placeholder (rỗng) chỉ dùng để ô Spinner lúc ĐÓNG
                // hiển thị trống - còn khi bấm mở danh sách xổ xuống thì ẨN HẲN dòng đó đi
                // (cao 0, không bấm được) để người dùng không thấy/chọn nhầm 1 dòng trống.
                adapter = object : ArrayAdapter<SelectItem>(context, R.layout.kr_spinner_default, R.id.text, displayOptions) {
                    override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                        if (hasNoSelection && position == 0) {
                            return convertView?.takeIf { it.tag == "kr_spinner_placeholder_hidden" }
                                ?: View(context).apply {
                                    layoutParams = android.widget.AbsListView.LayoutParams(0, 0)
                                    tag = "kr_spinner_placeholder_hidden"
                                }
                        }
                        // Tránh crash "Failed to find view with ID .../text": convertView có thể là
                        // view placeholder ẩn (View trống, không có id/text) bị recycle từ position 0
                        // sang đây. Nếu vậy, không tái sử dụng nó, để ArrayAdapter tự inflate view mới.
                        val safeConvertView = convertView?.takeIf { it.tag != "kr_spinner_placeholder_hidden" }
                        return super.getDropDownView(position, safeConvertView, parent)
                    }

                    override fun areAllItemsEnabled(): Boolean = !hasNoSelection

                    override fun isEnabled(position: Int): Boolean {
                        return !(hasNoSelection && position == 0)
                    }
                }.apply {
                    setDropDownViewResource(R.layout.kr_spinner_dropdown)
                }
                isEnabled = !actionParamInfo.readonly

                setSelection(if (hasNoSelection) 0 else selectedIndex + indexOffset)

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedIndex = pos - indexOffset
                        onValueChanged?.invoke()
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }

            return layout
        }
    }

    private fun openSingleSelectDialog(valueView: TextView, textView: TextView) {
        // >>> CHẶN TẠI ĐÂY: Tránh việc nhấn nhanh mở 2 DialogItemChooser cùng lúc
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 1000) {
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