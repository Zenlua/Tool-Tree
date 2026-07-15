package com.omarea.krscript.ui

import android.content.Context
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.PageMenuOption
import com.omarea.krscript.model.PageNode

class PageMenuLoader(private val applicationContext: Context, private val pageNode: PageNode) {

    // Không cần dùng biến menuOptions ở đây nữa để tránh giữ lại trạng thái cũ

    fun load(): ArrayList<PageMenuOption>? {
        val resultList = ArrayList<PageMenuOption>()
        
        // Ưu tiên script
        if (pageNode.pageMenuOptionsSh.isNotEmpty()) {
            val result = ScriptEnvironmen.executeResultRoot(
                applicationContext,
                pageNode.pageMenuOptionsSh,
                pageNode
            )
    
            if (result != "error") {
                val items = result.split("\n")
                for (item in items) {
                    val line = item.trim()
                    if (line.isEmpty()) continue
                    val option = PageMenuOption(pageNode.pageConfigPath)
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) {
                        option.key = parts[0].trim()
                        option.title = parts[1].trim()
                    } else {
                        option.key = line
                        option.title = line
                    }
                    resultList.add(option)
                }
            }
        }
    
        // Quyết định kết quả trả về trực tiếp (fallback)
        return if (resultList.isEmpty() && pageNode.pageMenuOptions != null) {
            pageNode.pageMenuOptions
        } else {
            resultList
        }
    }
}
