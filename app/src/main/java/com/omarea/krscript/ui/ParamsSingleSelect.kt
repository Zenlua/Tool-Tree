package com.omarea.krscript.ui

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        private val onValueChanged: (() -> Unit)? = null
) {

    private val darkMode: Boolean = ThemeModeState.isDarkMode()
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options)

    private var lastOpenTime: Long = 0
    private var anchorView: TextView? = null

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
}
