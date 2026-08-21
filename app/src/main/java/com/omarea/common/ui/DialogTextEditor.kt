package com.omarea.common.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.tool.tree.R

/**
 * Dialog toàn màn hình để chỉnh sửa văn bản dài, dùng cho ô nhập (ParamsEditText)
 * khi nội dung vượt quá 4 dòng - tương tự cách DialogItemChooser mở toàn màn hình
 * cho chế độ chọn nhiều mục, nhưng ở đây là cho phép nhập/sửa văn bản tự do
 * với nhiều không gian hơn.
 */
class DialogTextEditor(
    darkMode: Boolean,
    private val title: String?,
    private val initialText: String,
    private val callback: Callback? = null
) : DialogFullScreen(R.layout.dialog_text_editor, darkMode) {

    interface Callback {
        fun onConfirm(text: String)
    }

    private var editText: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        if (!title.isNullOrEmpty()) {
            titleView.text = title
            titleView.visibility = View.VISIBLE
        } else {
            titleView.visibility = View.GONE
        }

        editText = view.findViewById<EditText>(R.id.kr_text_editor_content).apply {
            setText(initialText)
            // Đặt con trỏ về cuối văn bản để tiện sửa tiếp
            setSelection(text?.length ?: 0)
            requestFocus()
        }

        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }
        view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
            callback?.onConfirm(editText?.text?.toString() ?: "")
            dismiss()
        }
    }
}
