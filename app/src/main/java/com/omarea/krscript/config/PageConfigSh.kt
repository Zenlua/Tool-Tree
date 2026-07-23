package com.omarea.krscript.config

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.tool.tree.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.PageNode
import java.io.ByteArrayInputStream

class PageConfigSh(private var activity: Activity, private var pageConfigSh: String, private var parentConfig: PageNode?) {
    private var handler = Handler(Looper.getMainLooper())

    // Nhận diện nội dung TOML inline khi dòng 1 hoặc dòng 2 (bỏ qua dòng trống) là
    // header bắt đầu bằng từ khoá "group" - vd: [[group]], [[group.action]] ...
    // (chỉ khớp đúng "group" để tránh nhận nhầm output lỗi/không liên quan của script).
    private fun looksLikeInlineToml(result: String): Boolean {
        val firstLines = result.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.take(2).toList()
        return firstLines.any { it.startsWith("[[group]]") || it.startsWith("[[group.") }
    }

    private fun pageConfigShError(content: String) {
        handler.post {
            Toast.makeText(activity, activity.getString(R.string.kr_page_sh_invalid) + "\n" + content, Toast.LENGTH_LONG).show()
        }
    }

    private fun noReadPermission() {
        handler.post {
            Toast.makeText(activity, activity.getString(R.string.kr_page_sh_file_permission), Toast.LENGTH_LONG).show()
        }
    }

    fun execute(): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null

        val result = ScriptEnvironmen.executeResultRoot(activity, pageConfigSh, parentConfig)?.trim()
        if (result != null) {
            if (result.endsWith(".xml") || result.endsWith(".toml")) {
                items = PageConfigReader(activity, result, parentConfig?.pageConfigDir).readConfigXml()
                if (items == null) {
                    noReadPermission()
                }
            } else if (result.startsWith("<?xml") && result.endsWith(">")) {
                val inputStream = ByteArrayInputStream(result.toByteArray())
                items = PageConfigReader(activity, inputStream).readConfigXml()
            } else if (looksLikeInlineToml(result)) {
                // Nội dung TOML trả về trực tiếp (không phải đường dẫn file):
                // nhận diện qua header [[group]]/[[group. ...]] ở dòng 1 hoặc dòng 2.
                val inputStream = ByteArrayInputStream(result.toByteArray())
                items = PageConfigReader(activity, inputStream).readConfigXml()
            } else if (result.isNotEmpty()) {
                pageConfigShError(result)
            }
        }
        return items
    }
}
