package com.omarea.krscript.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.tool.tree.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.NodeInfoBase
import java.util.regex.Pattern

open class ListItemView(private val context: Context,
                        layoutId: Int,
                        private val config: NodeInfoBase) {
    protected var layout = LayoutInflater.from(context).inflate(layoutId, null)

    protected var descView = layout.findViewById<TextView?>(R.id.kr_desc)
    protected var summaryView: TextView? = layout.findViewById(R.id.kr_summary)
    protected var titleView = layout.findViewById<TextView?>(R.id.kr_title)


    val key: String
        get() {
            return config.key
        }

    var title: String
        get() {
            return titleView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                titleView?.visibility = View.GONE
            } else {
                titleView?.text = value
                titleView?.visibility = View.VISIBLE
            }
        }

    var desc: String
        get() {
            return descView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                descView?.visibility = View.GONE
            } else {
                descView?.text = value
                descView?.visibility = View.VISIBLE
            }
        }

    var summary: String
        get() {
            return summaryView?.text.toString()
        }
        set(value) {
            if (value.isEmpty()) {
                summaryView?.visibility = View.GONE
            } else {
                summaryView?.text = value
                summaryView?.visibility = View.VISIBLE
            }
        }

    val index: String
        get() {
            return config.index
        }

    // Hàm bổ trợ đã cải tiến bằng Regex để tự dọn rác và cắt chuỗi chính xác tuyệt đối
    private fun resolveString(value: String?): String {
        if (value == null) return ""
        
        // Loại bỏ khoảng trắng hoặc ký tự xuống dòng thừa ở hai đầu
        val cleanedValue = value.trim()

        // Sử dụng Regex để tìm mẫu @string/tên hoặc @string:tên
        // Mẫu này sẽ bắt trích xuất chính xác phần tên resource phía sau
        val pattern = Pattern.compile("@string[/:](\\w+)")
        val matcher = pattern.matcher(cleanedValue)

        if (matcher.find()) {
            // matcher.group(1) sẽ là phần tên thuần túy (ví dụ: script_action_check_ext4_image)
            val stringName = matcher.group(1)
            
            val resId = context.resources.getIdentifier(stringName, "string", context.packageName)
            if (resId != 0) {
                return context.getString(resId)
            }
        }
        return cleanedValue
    }

    open fun updateViewByShell() {
        if (config.descSh.isNotEmpty()) {
            config.desc = ScriptEnvironmen.executeResultRoot(context, config.descSh, config)
            desc = resolveString(config.desc)
        }

        if (config.summarySh.isNotEmpty()) {
            config.summary = ScriptEnvironmen.executeResultRoot(context, config.summarySh, config)
            summary = resolveString(config.summary)
        }
    }

    fun getView(): View {
        return layout
    }

    init {
        title = resolveString(config.title)
        desc = resolveString(config.desc)
        summary = resolveString(config.summary)
    }
}
