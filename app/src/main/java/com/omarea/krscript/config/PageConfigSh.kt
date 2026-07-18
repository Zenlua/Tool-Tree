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
import java.io.File

class PageConfigSh(private var activity: Activity, private var pageConfigSh: String, private var parentConfig: PageNode?) {
    private var handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ASSETS_FILE = "file:///android_asset/"
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

    /**
     * Kiểm tra xem script có thực sự tồn tại hay không trước khi chạy.
     * - Nếu là đường dẫn asset (file:///android_asset/...) -> kiểm tra bằng AssetManager (rất nhanh, không tốn root).
     * - Nếu là đường dẫn tệp thật (bắt đầu bằng "/") -> kiểm tra bằng File.exists() (không tốn root).
     * - Nếu không phải là đường dẫn (ví dụ là nội dung lệnh shell viết trực tiếp trong cấu hình)
     *   thì coi như hợp lệ, giữ nguyên hành vi cũ.
     */
    private fun scriptFileExists(path: String): Boolean {
        return when {
            path.startsWith(ASSETS_FILE) -> {
                try {
                    activity.assets.open(path.substring(ASSETS_FILE.length)).use { true }
                } catch (ex: Exception) {
                    false
                }
            }
            path.startsWith("/") -> File(path).exists()
            else -> true
        }
    }

    fun execute(): ArrayList<NodeInfoBase>? {
        val script = pageConfigSh.trim()
        if (script.isEmpty()) {
            return null
        }

        // Nếu script được khai báo là 1 đường dẫn tệp cụ thể nhưng tệp đó không tồn tại
        // thì dừng ngay tại đây, không chạy root/shell -> tránh tốn thời gian cho tab không có tệp thật.
        if (!scriptFileExists(script)) {
            return null
        }

        var items: ArrayList<NodeInfoBase>? = null

        val result = ScriptEnvironmen.executeResultRoot(activity, pageConfigSh, parentConfig)?.trim()
        if (result != null) {
            if (result.endsWith(".xml")) {
                items = PageConfigReader(activity, result, parentConfig?.pageConfigDir).readConfigXml()
                if (items == null) {
                    noReadPermission()
                }
            } else if (result.startsWith("<?xml") && result.endsWith(">")) {
                val inputStream = ByteArrayInputStream(result.toByteArray())
                items = PageConfigReader(activity, inputStream).readConfigXml()
            } else if (result.isNotEmpty()) {
                pageConfigShError(result)
            }
        }
        return items
    }
}
