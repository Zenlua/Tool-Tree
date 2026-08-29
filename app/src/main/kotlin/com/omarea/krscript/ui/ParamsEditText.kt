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
            //
            // LƯU Ý (fix bug icon mở rộng hiện sai khi mới hiện qua depend-on):
            // Nếu EditText đang có width = 0 (chưa được đo thật sự - ví dụ đang nằm trong 1
            // row vừa GONE, hoặc dialog chưa layout xong), lineCount tính ra HOÀN TOÀN không
            // đáng tin (Android có thể wrap sai, trả về lineCount > 4 dù nội dung chỉ 1 dòng
            // ngắn). post{} lúc đó sẽ set nhầm expandBtn = VISIBLE và giá trị sai này bị "kẹt"
            // lại vì trước đây CHỈ afterTextChanged mới gọi lại hàm này - khi row được depend-on
            // hiện lên sau đó (không đổi text), không có gì kích hoạt tính lại, nên icon vẫn
            // hiện sai dù nội dung chỉ có 1 dòng.
            fun updateExpandButtonVisibility() {
                post {
                    if (width <= 0) {
                        // Chưa có width thật (view chưa được đo/đang ẩn) - lineCount không
                        // đáng tin, bỏ qua lần này. Sẽ được tính lại khi width thay đổi thật
                        // (xem addOnLayoutChangeListener bên dưới), ví dụ lúc depend-on hiện
                        // row lên và EditText được đo với kích thước thật.
                        return@post
                    }
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

            // Tính lại mỗi khi width THỰC SỰ đổi (ví dụ: view chuyển từ width=0 -> width thật
            // khi depend-on hiện row lên, hoặc xoay màn hình/đổi kích thước dialog). Đây là
            // phần quan trọng để sửa bug: trước đây chỉ có afterTextChanged trigger tính lại,
            // nên khi depend-on hiện 1 field đã tồn tại sẵn (không đổi text) thì lineCount sai
            // từ lần đo lúc width=0 không bao giờ được sửa lại.
            addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
                val newWidth = right - left
                val oldWidth = oldRight - oldLeft
                if (newWidth > 0 && newWidth != oldWidth) {
                    updateExpandButtonVisibility()
                }
            }

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