package com.omarea.krscript.ui

import android.text.Editable
import android.text.TextWatcher
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
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
        private val onValueChanged: (() -> Unit)? = null
) {

    private val darkMode: Boolean = ThemeModeState.isDarkMode()
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options)

    private var lastOpenTime: Long = 0
    private var anchorView: TextView? = null

    // ========== TÍNH NĂNG MỚI: SPINNER CHO PHÉP GÕ TAY / CHỈNH SỬA GIÁ TRỊ (editable) ==========
    // != null khi param khai báo editable="true" (giống file/folder param - xem
    // ParamsFileChooserRender): khi đó giao diện spinner chuyển sang dạng "combobox" -
    // 1 ô EditText cho gõ tay giá trị bất kỳ + nút mở danh sách ở bên phải (layout
    // kr_param_spinner_edit.xml mô phỏng đúng cấu trúc hàng của kr_param_file.xml).
    // Giá trị của param chính là nội dung ô nhập (WYSIWYG): chọn mục nào từ danh sách
    // thì VALUE của mục đó được điền thẳng vào ô, người dùng có thể tiếp tục sửa/bổ sung.
    // != null cũng là điều kiện để getValue() trả về nội dung ô nhập thay vì đọc theo
    // selectedIndex như spinner thường.
    private var editTextView: EditText? = null

    fun getValue(): String {
        // Spinner editable: giá trị chính là nội dung hiện tại của ô nhập - đảm bảo
        // valueReaders (dùng cho depend-on) và mọi nơi gọi widget.getValue() luôn thấy
        // đúng giá trị người dùng đang thấy/gõ, kể cả khi text là giá trị tự tay không
        // có sẵn trong danh sách options.
        editTextView?.let {
            return it.text?.toString() ?: ""
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

    private fun updateAnchorText(anchor: TextView) {
        if (selectedIndex > -1 && selectedIndex < options.size) {
            anchor.text = options[selectedIndex].title
            anchor.hint = null
        } else {
            anchor.text = null
            anchor.hint = context.getString(
                    if (options.isEmpty()) R.string.picker_not_item else R.string.kr_please_select
            )
        }
    }

    fun render(): View {
        // editable="true": render nhánh riêng (dùng chung cho CẢ danh sách ngắn lẫn dài -
        // chỉ khác nhau chỗ bấm nút sẽ mở popup dropdown hay DialogItemChooser, xem
        // renderEditable()). Các nhánh cũ bên dưới giữ nguyên 100% hành vi cũ.
        if (actionParamInfo.editable) {
            return renderEditable()
        }

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
            val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null) as ViewGroup

            val allowNoSelection = actionParamInfo.allowNoSelection
            if (!allowNoSelection && (selectedIndex < 0 || selectedIndex >= options.size) && options.isNotEmpty()) {
                selectedIndex = 0
            }

            // [SỬA LỖI] Tạo View ẩn mang TAG chứa VALUE thực tế cho KrScript quét đọc
            val valueHolder = TextView(context).apply {
                id = R.id.kr_param_value
                tag = actionParamInfo.name
                visibility = View.GONE
                text = getValue()
            }
            layout.addView(valueHolder)

            val anchor = layout.findViewById<TextView>(R.id.kr_param_spinner)
            anchorView = anchor
            updateAnchorText(anchor)

            val enabled = !actionParamInfo.readonly && options.isNotEmpty()
            anchor.isEnabled = enabled
            anchor.isClickable = enabled
            anchor.isFocusable = enabled

            if (enabled) {
                anchor.setOnClickListener {
                    openSingleSelectPopup(anchor, valueHolder)
                }
            }

            return layout
        }
    }

    private fun openSingleSelectPopup(anchor: TextView, valueHolder: TextView) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        val adapter = ArrayAdapter(context, R.layout.kr_spinner_dropdown, R.id.text, options)
        val background = context.getDrawable(R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(context)
        popup.anchorView = anchor
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(background)
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            updateAnchorText(anchor)
            
            // [SỬA LỖI] Cập nhật lại VALUE vào View ẩn khi đổi lựa chọn
            valueHolder.text = getValue()
            
            onValueChanged?.invoke()
            popup.dismiss()
        }

        applyPopupWidthAndPosition(popup, anchor, background)

        popup.show()
        if (selectedIndex > -1 && selectedIndex < options.size) {
            popup.listView?.setSelection(selectedIndex)
        }
    }

    private fun applyPopupWidthAndPosition(popup: ListPopupWindow, anchor: TextView, background: android.graphics.drawable.Drawable?) {
        val inflater = LayoutInflater.from(context)
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val parent = anchor.parent as? ViewGroup

        var maxItemWidth = 0
        for (item in options) {
            val itemView = inflater.inflate(R.layout.kr_spinner_dropdown, parent, false)
            itemView.findViewById<TextView>(R.id.text).text = item.title
            itemView.measure(unspecified, unspecified)
            if (itemView.measuredWidth > maxItemWidth) {
                maxItemWidth = itemView.measuredWidth
            }
        }

        val bgPadding = Rect()
        background?.getPadding(bgPadding)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val contentWidth = maxItemWidth + bgPadding.left + bgPadding.right
        val minWidth = anchor.width
        val desiredWidth = contentWidth.coerceAtLeast(minWidth).coerceAtMost(screenWidth)
        popup.width = desiredWidth

        val anchorLocation = IntArray(2)
        anchor.getLocationOnScreen(anchorLocation)
        val overflow = (anchorLocation[0] + desiredWidth) - screenWidth
        popup.horizontalOffset = if (overflow > 0) -overflow else 0
    }

    private fun openSingleSelectDialog(valueView: TextView, textView: TextView) {
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
                selectedIndex = status.indexOf(true)
                updateValueView(valueView, textView)
                onValueChanged?.invoke()
            }
        }).show(context.supportFragmentManager, "params-single-select")
    }

    // ==================================================================================
    // ========== TÍNH NĂNG MỚI: CÁC METHOD CHO SPINNER EDITABLE (editable="true") =======
    // ==================================================================================

    // Render hàng spinner dạng "combobox" - mô phỏng cấu trúc/đặc điểm của param file/folder
    // (ParamsFileChooserRender + kr_param_file.xml):
    // - Ô EditText chiếm gần hết bề ngang, gõ tay được, MANG TAG = tên param để
    //   ActionParamsLayoutRender.readParamsValue() quét đọc đúng giá trị khi chạy action
    //   (readParamsValue đã có sẵn nhánh `is EditText` xử lý theo tag, không cần sửa gì).
    // - Nút mũi tên xuống bên phải (cùng vạch ngăn mảnh như nút thư mục của file/folder):
    //   danh sách NGẮN (<=6 mục) mở popup ListPopupWindow neo tại ô nhập (EditText là
    //   con của TextView nên làm anchor được luôn); danh sách DÀI (>6 mục) mở
    //   DialogItemChooser - đúng cách chia nhánh của spinner thường.
    // - Chọn 1 mục: điền VALUE của mục đó thẳng vào ô nhập (WYSIWYG - ô nhập luôn chứa
    //   đúng chuỗi sẽ gửi cho script, giống ô đường dẫn của file/folder), con trỏ nhảy
    //   về cuối để người dùng tiện tiếp tục sửa/bổ sung.
    // - Gõ tay bất kỳ chữ nào: đồng bộ lại selectedIndex nếu text trùng value của 1 option
    //   (để lần mở danh sách sau highlight đúng vị trí) và gọi onValueChanged để các
    //   param depend-on param này cập nhật ẩn/hiện NGAY như khi gõ vào ô path file/folder.
    private fun renderEditable(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner_edit, null)
        val editText = layout.findViewById<EditText>(R.id.kr_param_spinner_edit)
        val btn = layout.findViewById<View>(R.id.kr_param_spinner_btn)

        editText.tag = actionParamInfo.name
        editTextView = editText

        // Giá trị ban đầu: ưu tiên kết quả value-sh rồi tới value tĩnh (giống file/folder).
        // Nếu không khớp option nào thì selectedIndex = -1 (không highlight nhầm mục nào).
        // LƯU Ý: KHÔNG được gọi getValue() ở đây - vì editTextView đã được gán ngay trước đó
        // nên getValue() sẽ trả về text hiện tại của ô nhập (đang rỗng) thay vì giá trị thật
        // của option[selectedIndex]. Phải lấy thẳng từ options[] để điền giá trị mặc định.
        val initialValue = actionParamInfo.valueFromShell
            ?: actionParamInfo.value
            ?: ""
        if (initialValue.isNotEmpty()) {
            editText.setText(initialValue)
            val matchIndex = options.indexOfFirst { it.value == initialValue }
            selectedIndex = if (matchIndex >= 0) matchIndex else -1
        } else if (!actionParamInfo.allowNoSelection && options.isNotEmpty()) {
            // Giữ đúng hành vi Spinner mặc định (không có allow-no-selection): chưa có
            // giá trị nào thì tự chọn sẵn mục đầu tiên và điền VALUE của nó vào ô nhập.
            selectedIndex = 0
            editText.setText(options[0].value ?: "")
        }

        // placeholder riêng nếu cấu hình khai báo (giống ParamsEditText), không thì giữ
        // hint mặc định "Vui lòng chọn" từ layout.
        if (actionParamInfo.placeholder.isNotEmpty()) {
            editText.hint = actionParamInfo.placeholder
        }

        // readonly: khóa ô nhập (vẫn thấy được giá trị) và khóa nút mở danh sách.
        val enabled = !actionParamInfo.readonly && options.isNotEmpty()
        editText.isEnabled = !actionParamInfo.readonly
        btn.isEnabled = enabled
        if (enabled) {
            btn.setOnClickListener {
                if (options.size > 6) {
                    openSingleSelectDialogEditable(editText)
                } else {
                    openSingleSelectPopupEditable(editText)
                }
            }
        }

        // Mọi thay đổi trên ô nhập (gõ/xoá/dán): đồng bộ selectedIndex khi text trùng
        // value của 1 option, rồi báo onValueChanged để depend-on đánh giá lại ngay.
        // (selectedIndex ở chế độ này chỉ phục vụ highlight mục khi mở danh sách - giá
        // trị thật luôn lấy từ nội dung ô nhập, xem getValue()).
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                val matchIndex = options.indexOfFirst { it.value == text }
                if (matchIndex >= 0) {
                    selectedIndex = matchIndex
                }
                onValueChanged?.invoke()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return layout
    }

    // Popup dropdown cho spinner editable (danh sách ngắn <=6 mục) - giữ nguyên toàn bộ
    // cơ chế của openSingleSelectPopup() (debounce 800ms, ListPopupWindow + nền
    // kr_spinner_popup_bg, độ rộng đo theo nội dung + tự lùi khi tràn mép màn hình,
    // highlight mục đang chọn) - chỉ khác: neo vào ô nhập và khi bấm chọn mục thì điền
    // VALUE vào ô nhập (thay vì cập nhật TextView + valueHolder ẩn như bản thường).
    private fun openSingleSelectPopupEditable(editText: EditText) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        val adapter = ArrayAdapter(context, R.layout.kr_spinner_dropdown, R.id.text, options)
        val background = context.getDrawable(R.drawable.kr_spinner_popup_bg)

        val popup = ListPopupWindow(context)
        popup.anchorView = editText
        popup.setAdapter(adapter)
        popup.setBackgroundDrawable(background)
        popup.isModal = true
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            val selectedValue = options.getOrNull(position)?.value ?: ""
            editText.setText(selectedValue)
            editText.setSelection(editText.text?.length ?: 0)
            onValueChanged?.invoke()
            popup.dismiss()
        }

        applyPopupWidthAndPosition(popup, editText, background)

        popup.show()
        if (selectedIndex > -1 && selectedIndex < options.size) {
            popup.listView?.setSelection(selectedIndex)
        }
    }

    // Dialog chọn cho spinner editable (danh sách dài >6 mục) - giữ nguyên cơ chế của
    // openSingleSelectDialog() (debounce 800ms, DialogItemChooser single-select) - chỉ
    // khác: khi bấm Xác nhận, VALUE của mục được chọn được điền thẳng vào ô nhập thay vì
    // cập nhật cặp TextView/valueHolder ẩn của bản thường.
    private fun openSingleSelectDialogEditable(editText: EditText) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOpenTime < 800) {
            return
        }
        lastOpenTime = currentTime

        DialogItemChooser(darkMode, ArrayList(options.mapIndexed { index, item ->
            SelectItem().apply {
                title = item.title
                selected = index == selectedIndex
            }
        }), false, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                val confirmedIndex = status.indexOf(true)
                if (confirmedIndex > -1 && confirmedIndex < options.size) {
                    selectedIndex = confirmedIndex
                    editText.setText(options[confirmedIndex].value ?: "")
                    editText.setSelection(editText.text?.length ?: 0)
                    onValueChanged?.invoke()
                }
            }
        }).show(context.supportFragmentManager, "params-single-select-edit")
    }
}
