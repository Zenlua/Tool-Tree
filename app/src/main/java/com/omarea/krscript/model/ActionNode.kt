package com.omarea.krscript.model

import java.util.*

class ActionNode(currentConfigXml: String) : RunnableNode(currentConfigXml){
    var params: ArrayList<ActionParamInfo>? = null
    // Giống text.rows: cho phép action hiển thị thêm các dòng văn bản rich-text bên dưới (rows)
    val rows = ArrayList<TextNode.TextRow>()
}
