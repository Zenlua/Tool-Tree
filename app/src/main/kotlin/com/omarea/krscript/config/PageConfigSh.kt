package com.omarea.krscript.config

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.tool.tree.R
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ActionNode
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.PageMenuOption
import com.omarea.krscript.model.PageNode
import java.io.ByteArrayInputStream

class PageConfigSh(private var activity: Activity, private var pageConfigSh: String, private var parentConfig: PageNode?) {
    private var handler = Handler(Looper.getMainLooper())

    // Reader được dùng ở lần execute() gần nhất - dùng để lấy pageMenuOptions ([[menu]]/[[fab]])
    // được gom trong lúc parse, GIỐNG hệt cách ActionPage lấy từ PageConfigReader ở nhánh
    // pageConfigPath (file .toml tĩnh). execute() có thể không tạo reader nào (vd script lỗi/rỗng)
    // nên luôn fallback về danh sách rỗng thay vì null.
    private var lastReader: PageConfigReader? = null
    val pageMenuOptions: ArrayList<PageMenuOption> get() = lastReader?.pageMenuOptions ?: ArrayList()
    val headerActions: ArrayList<ActionNode> get() = lastReader?.headerActions ?: ArrayList()
    val autoShowActions: ArrayList<ActionNode> get() = lastReader?.autoShowActions ?: ArrayList()

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

    fun execute(onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null

        val result = ScriptEnvironmen.executeResultRoot(activity, pageConfigSh, parentConfig)?.trim()
        if (result != null) {
            if (result.endsWith(".toml")) {
                val reader = PageConfigReader(activity, result, parentConfig?.pageConfigDir)
                lastReader = reader
                items = reader.readConfigXml(onNodeReady)
                if (items == null) {
                    noReadPermission()
                }
            } else if (looksLikeInlineToml(result)) {
                // Nội dung TOML trả về trực tiếp (không phải đường dẫn file):
                // nhận diện qua header [[group]]/[[group. ...]] ở dòng 1 hoặc dòng 2.
                val inputStream = ByteArrayInputStream(result.toByteArray())
                val reader = PageConfigReader(activity, inputStream)
                lastReader = reader
                items = reader.readConfigXml(onNodeReady)
            } else if (result.isNotEmpty()) {
                pageConfigShError(result)
            }
        }
        return items
    }

    // process = true: đọc output shell THEO DÒNG (qua ScriptEnvironmen.executeStreamingRoot),
    // ngay khi 1 khối top-level (từ 1 header như "[[group]]" tới trước header top-level tiếp
    // theo - xem PageConfigReader.TOP_LEVEL_HEADER_REGEX) xuất hiện ĐẦY ĐỦ là parse & trả về
    // ngay qua onNodeReady - KHÔNG đợi các lệnh chạy sau nó (vd sleep) trong script hoàn tất.
    // Khối nào lỗi cú pháp chỉ khối đó hiện 1 mục báo lỗi tại đúng vị trí, không ảnh hưởng các
    // khối khác - xem PageConfigReader.readConfigTomlBlock().
    // Nếu suốt output không có header top-level nào (vd script chỉ trả về 1 dòng là đường dẫn
    // .toml) thì fallback nguyên vẹn về hành vi như execute() (đọc file/parse nguyên khối).
    fun executeStreaming(onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase>? {
        val streamReader = PageConfigReader(activity, parentConfig?.pageConfigDir)
        lastReader = streamReader

        val items = ArrayList<NodeInfoBase>()
        var done = 0
        val rawAll = StringBuilder()
        var currentBlock: StringBuilder? = null
        var blockStarted = false

        fun flush(blockText: String) {
            for (node in streamReader.readConfigTomlBlock(blockText)) {
                done++
                items.add(node)
                onNodeReady?.invoke(node, done, -1)
            }
        }

        ScriptEnvironmen.executeStreamingRoot(activity, pageConfigSh, parentConfig) { line ->
            rawAll.append(line).append("\n")
            val trimmed = line.trim()
            if (PageConfigReader.TOP_LEVEL_HEADER_REGEX.matches(trimmed)) {
                currentBlock?.let { if (it.isNotBlank()) flush(it.toString()) }
                currentBlock = StringBuilder(line).append("\n")
                blockStarted = true
            } else if (blockStarted) {
                currentBlock?.append(line)?.append("\n")
            }
            // Dòng xuất hiện TRƯỚC header top-level đầu tiên (nếu có) bị bỏ qua ở đây - chỉ
            // được xét lại (nguyên chuỗi rawAll) nếu cả stream không có header nào, xem dưới.
        }
        currentBlock?.let { if (it.isNotBlank()) flush(it.toString()) }

        if (blockStarted) {
            return items
        }

        // Không có khối top-level nào trong toàn bộ output -> không phải dạng streaming
        // nhiều khối, fallback về đúng hành vi cũ của execute().
        val result = rawAll.toString().trim()
        return when {
            result.endsWith(".toml") -> {
                val reader = PageConfigReader(activity, result, parentConfig?.pageConfigDir)
                lastReader = reader
                val fallbackItems = reader.readConfigXml(onNodeReady)
                if (fallbackItems == null) {
                    noReadPermission()
                }
                fallbackItems
            }
            looksLikeInlineToml(result) -> {
                val inputStream = ByteArrayInputStream(result.toByteArray())
                val reader = PageConfigReader(activity, inputStream)
                lastReader = reader
                reader.readConfigXml(onNodeReady)
            }
            result.isNotEmpty() -> {
                pageConfigShError(result)
                items
            }
            else -> items
        }
    }
}