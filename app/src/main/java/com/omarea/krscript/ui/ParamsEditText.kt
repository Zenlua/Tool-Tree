package com.omarea.krscript.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.FragmentActivity
import com.omarea.common.ui.DialogTextEditor
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import com.omarea.krscript.model.ParamInfoFilter
import com.tool.tree.ThemeModeState

class ParamsEditText(private var actionParamInfo: ActionParamInfo, private var context: FragmentActivity) {
    companion object {
        // Ngưỡng số dòng: vượt quá thì hiện nút mở rộng toàn màn hình
        private const val EXPAND_LINE_THRESHOLD = 4
    }

    private val darkMode: Boolean = ThemeModeState.isDarkMode()

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_text, null)
        val expandBtn = layout.findViewById<ImageButton>(R.id.kr_param_text_expand)
        // Chụp lại vào biến local: bên trong `run{}` trên EditText, tên "context" sẽ bị
        // shadow bởi View.getContext() (kiểu Context thường, không có supportFragmentManager),
        // nên phải dùng activity riêng để mở DialogFragment.
        val activity = context

        layout.findViewById<EditText>(R.id.kr_param_text).run {
            tag = actionParamInfo.name
            if (actionParamInfo.valueFromShell != null)
                setText(actionParamInfo.valueFromShell)
            else if (actionParamInfo.value != null) {
                setText(actionParamInfo.value)
            }
            filters = arrayOf(ParamInfoFilter(actionParamInfo))
            isEnabled = !actionParamInfo.readonly
            if (actionParamInfo.placeholder.isNotEmpty()) {
                hint = actionParamInfo.placeholder
            } else if (
                    (actionParamInfo.type == "int" || actionParamInfo.type == "number")
                    &&
                    (actionParamInfo.min != Int.MIN_VALUE || actionParamInfo.max != Int.MAX_VALUE)
            ) {
                hint = "${actionParamInfo.min} ~ ${actionParamInfo.max}"
            }

            // ===== Hỗ trợ mở toàn màn hình khi nội dung vượt quá 4 dòng =====
            // Dùng post{} để đọc lineCount SAU khi layout đã tính toán xong (bao gồm cả
            // trường hợp xuống dòng tự động do wrap text, không chỉ do ký tự \n).
            fun updateExpandButtonVisibility() {
                post {
                    expandBtn.visibility = if (lineCount > EXPAND_LINE_THRESHOLD) View.VISIBLE else View.GONE
                }
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateExpandButtonVisibility()
                }
            })
            // Kiểm tra ngay khi render (trường hợp có sẵn giá trị dài từ trước)
            updateExpandButtonVisibility()

            expandBtn.setOnClickListener {
                val editText = this
                DialogTextEditor(
                        darkMode,
                        (actionParamInfo.title ?: actionParamInfo.label ?: actionParamInfo.placeholder).let { it.ifEmpty { null } },
                        editText.text?.toString() ?: "",
                        object : DialogTextEditor.Callback {
                            override fun onConfirm(text: String) {
                                editText.setText(text)
                                editText.setSelection(editText.text?.length ?: 0)
                            }
                        }
                ).show(activity.supportFragmentManager, "params-text-editor")
            }
        }

        return layout
    }
}
