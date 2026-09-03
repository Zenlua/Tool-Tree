package com.omarea.common.ui

import android.os.Bundle
import android.text.InputFilter
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

    // ========== CẤU HÌNH TRUYỀN TỪ BÊN NGOÀI (ParamsEditText) ==========
    // DialogFragment chỉ "thật sự" dựng view SAU KHI show() được gọi, nên các cấu hình
    // truyền qua setter TRƯỚC .show() phải được LƯU LẠI vào các trường của class trước,
    // rồi mới áp dụng vào EditText trong onViewCreated(). Gán thẳng vào view trong
    // setter là vô nghĩa vì lúc đó findViewById() chưa thể trả về EditText nào.

    // Placeholder/hint của EditText gốc (nếu có) - hiển thị khi ô nhập trong dialog trống
    private var editHint: CharSequence? = null
    // Kiểu nhập (InputType) đồng bộ với EditText gốc: số nguyên / số thập phân / văn bản nhiều dòng
    private var editInputType: Int? = null
    // Bộ lọc ký tự (InputFilter) đồng bộ với EditText gốc: giới hạn độ dài, ràng buộc kiểu số...
    private var editFilters: List<InputFilter> = emptyList()

    /** Truyền placeholder/hint lấy từ EditText gốc vào dialog (lưu lại, áp dụng khi view được dựng) */
    fun setHint(hint: CharSequence?) {
        editHint = hint
    }

    /** Truyền kiểu nhập (InputType) lấy từ EditText gốc vào dialog */
    fun setInputType(type: Int) {
        editInputType = type
    }

    /** Truyền bộ lọc ký tự (InputFilter) lấy từ EditText gốc vào dialog */
    fun setFilters(filters: Array<out InputFilter>) {
        editFilters = filters.toList()
    }

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
            // Áp dụng các cấu hình truyền từ bên ngoài TRƯỚC khi gán văn bản,
            // tránh việc đổi inputType/filter sau setText() làm reset con trỏ:
            // 1. Placeholder/hint lấy từ EditText gốc - hiển thị khi ô nhập trống
            editHint?.let { hint = it }
            // 2. Kiểu nhập đồng bộ với EditText gốc (số nguyên / số thập phân / văn bản nhiều dòng)
            editInputType?.let { inputType = it }
            // 3. Bộ lọc ký tự đồng bộ với EditText gốc (maxLength, ràng buộc int/number...)
            if (editFilters.isNotEmpty()) {
                filters = editFilters.toTypedArray()
            }

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
