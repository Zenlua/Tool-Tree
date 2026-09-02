package com.omarea.krscript.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import com.omarea.common.ui.DialogTextEditor
import com.tool.tree.R
import com.omarea.krscript.model.ActionParamInfo
import com.omarea.krscript.model.ParamInfoFilter
import com.tool.tree.ThemeModeState

class ParamsEditText(private var actionParamInfo: ActionParamInfo, private var context: FragmentActivity) {
    private val darkMode: Boolean = ThemeModeState.isDarkMode()

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_text, null)
        // Icon mở toàn màn hình: luôn hiện (không còn giới hạn theo số dòng nữa)
        val expandBtn = layout.findViewById<View>(R.id.kr_param_text_expand)
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

            expandBtn.setOnClickListener {
                val editText = this
                // Tiêu đề dialog: ghép title + label (nếu có), giống cách getFieldTips() ghép
                // title/label ở nơi khác trong codebase; nếu không có title/label thì dùng placeholder.
                val titleParts = listOfNotNull(
                        actionParamInfo.title?.ifEmpty { null },
                        actionParamInfo.label?.ifEmpty { null }
                )
                val dialogTitle = if (titleParts.isNotEmpty()) {
                    titleParts.joinToString(" ")
                } else {
                    null
                }

                DialogTextEditor(
                        darkMode,
                        dialogTitle,
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
