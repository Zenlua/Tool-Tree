package com.omarea.krscript.config

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.util.Log
import android.util.Xml
import android.widget.Toast
import com.omarea.common.model.SelectItem
import com.omarea.krscript.executor.ExtractAssets
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale.getDefault
import androidx.core.graphics.toColorInt
// Thư viện phân tích TOML (cần thêm vào build.gradle, ví dụ: implementation("org.tomlj:tomlj:1.1.1"))
import org.tomlj.Toml
import org.tomlj.TomlTable

/**
 * Created by Hello on 2018/04/01.
 */
class PageConfigReader {
    private var context: Context
    private var pageConfig: String = ""

    // 读取pageConfig时自动获得
    private var pageConfigAbsPath: String = ""
    private var pageConfigStream: InputStream? = null
    private var parentDir: String = ""

    private fun sanitizeXmlContent(raw: String): String {
        var result = raw
    
        // 1. Loại bỏ các ký tự không hợp lệ trong XML 1.0
        // Giữ lại: TAB (\u0009), LF (\u000A), CR (\u000D)
        result = result.replace(
            Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uD800-\\uDFFF\\uFFFE\\uFFFF]"),
            ""
        )
    
        // 2. Escape '&' nếu chưa phải entity hợp lệ
        result = result.replace(
            Regex("&(?!(?:amp|lt|gt|quot|apos|#[0-9]+|#x[0-9A-Fa-f]+);)"),
            "&amp;"
        )
    
        // 3. Escape '<' nếu không phải mở đầu markup XML hợp lệ
        result = result.replace(
            Regex("<(?!/?[A-Za-z_]|!|\\?)"),
            "&lt;"
        )
    
        // 4. Escape '>' chỉ khi tạo thành "]]>" ngoài CDATA
        result = result.replace("]]>", "]]&gt;")
    
        // 5. Quy ước xuống dòng
        result = result.replace("§", "&#xA;")
    
        return result
    }

    constructor(context: Context, pageConfig: String, parentDir: String?) {
        this.context = context
        this.pageConfig = pageConfig
        this.parentDir = parentDir ?: ""
    }

    constructor(context: Context, pageConfigStream: InputStream) {
        this.context = context
        this.pageConfigStream = pageConfigStream
    }

    fun readConfigXml(): ArrayList<NodeInfoBase>? {
        if (pageConfigStream != null) {
            return readConfigXml(pageConfigStream!!)
        } else {
            try {
                val pathAnalysis = PathAnalysis(context, parentDir)
                pathAnalysis.parsePath(pageConfig).run {
                    val fileInputStream = this ?: return ArrayList()
                    pageConfigAbsPath = pathAnalysis.getCurrentAbsPath()
                    return readConfigXml(fileInputStream)
                }
            } catch (ex: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to parse configuration file\n" + ex.message, Toast.LENGTH_LONG).show()
                }
                Log.e("KrConfig Fail！", "" + ex.message)
            }

        }
        return null
    }

    // Xác định file cấu hình là TOML hay XML.
    // Ưu tiên theo phần mở rộng file (không đổi hành vi các file .xml/không rõ đuôi hiện có);
    // chỉ khi không xác định được qua đường dẫn (ví dụ đọc trực tiếp từ InputStream) mới đoán qua nội dung.
    private fun isTomlConfig(pathHint: String, rawText: String): Boolean {
        val hint = pathHint.lowercase(getDefault())
        if (hint.endsWith(".toml")) return true
        if (hint.endsWith(".xml")) return false
        val firstChar = rawText.trimStart { it.isWhitespace() || it == '\uFEFF' }.firstOrNull()
        return firstChar != null && firstChar != '<'
    }

    private fun readConfigXml(fileInputStream: InputStream): ArrayList<NodeInfoBase>? {
        try {
            // Đọc toàn bộ nội dung, sửa các ký tự dễ gây lỗi XML, rồi mới đưa vào trình phân tích
            val rawText = fileInputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            if (isTomlConfig(pageConfigAbsPath, rawText)) {
                return readConfigToml(rawText)
            }

            val sanitizedText = sanitizeXmlContent(rawText)
            val sanitizedStream = ByteArrayInputStream(sanitizedText.toByteArray(Charsets.UTF_8))

            val parser = Xml.newPullParser()// 获取xml解析器
            parser.setInput(sanitizedStream, "utf-8")// 参数分别为输入流和字符编码
            var type = parser.eventType
            val mainList: ArrayList<NodeInfoBase> = ArrayList()
            var action: ActionNode? = null
            var switch: SwitchNode? = null
            var picker: PickerNode? = null
            var group: GroupNode? = null
            var page: PageNode? = null
            var text: TextNode? = null
            var editor: EditorNode? = null
            var isRootNode = true
            while (type != XmlPullParser.END_DOCUMENT) { // 如果事件不等于文档结束事件就继续循环
                when (type) {
                    XmlPullParser.START_TAG -> {
                        if ("group" == parser.name) {
                            if (group != null && group.supported) {
                                mainList.add(group)
                            }
                            group = groupNode(parser)
                        } else if (group != null && !group.supported) {
                            // 如果 group.supported !- true 跳过group内所有项
                        } else {
                            if ("page" == parser.name) {
                                if (!isRootNode) {
                                    page = clickbleNode(PageNode(pageConfigAbsPath), parser) as PageNode?
                                    if (page != null) {
                                        page = pageNode(page, parser)
                                    }
                                }
                            } else if ("action" == parser.name) {
                                action = runnableNode(ActionNode(pageConfigAbsPath), parser) as ActionNode?
                            } else if ("switch" == parser.name) {
                                switch = runnableNode(SwitchNode(pageConfigAbsPath), parser) as SwitchNode?
                            } else if ("picker" == parser.name) {
                                picker = runnableNode(PickerNode(pageConfigAbsPath), parser) as PickerNode?
                                if (picker != null) {
                                    pickerNode(picker, parser)
                                }
                            } else if ("text" == parser.name) {
                                text = mainNode(TextNode(pageConfigAbsPath), parser) as TextNode?
                            } else if ("editor" == parser.name) {
                                editor = clickbleNode(EditorNode(pageConfigAbsPath), parser) as EditorNode?
                                if (editor != null) {
                                    editor = editorNode(editor, parser)
                                }
                            } else if (page != null) {
                                tagStartInPage(page, parser)
                            } else if (action != null) {
                                tagStartInAction(action, parser)
                            } else if (switch != null) {
                                tagStartInSwitch(switch, parser)
                            } else if (picker != null) {
                                tagStartInPicker(picker, parser)
                            } else if (text != null) {
                                tagStartInText(text, parser)
                            } else if ("resource" == parser.name) {
                                resourceNode(parser)
                            }
                        }
                        isRootNode = false
                    }
                    XmlPullParser.END_TAG ->
                        if ("group" == parser.name) {
                            if (group != null && group.supported) {
                                mainList.add(group)
                            }
                            group = null
                        } else if (group != null) {
                            when (parser.name) {
                                "page" -> {
                                    tagEndInPage(page, parser)
                                    if (page != null) {
                                        group.children.add(page)
                                    }
                                    page = null
                                }
                                "action" -> {
                                    tagEndInAction(action, parser)
                                    if (action != null) {
                                        group.children.add(action)
                                    }
                                    action = null
                                }
                                "switch" -> {
                                    tagEndInSwitch(switch, parser)
                                    if (switch != null) {
                                        group.children.add(switch)
                                    }
                                    switch = null
                                }
                                "picker" -> {
                                    tagEndInPicker(picker, parser)
                                    if (picker != null) {
                                        group.children.add(picker)
                                    }
                                    picker = null
                                }
                                "text" -> {
                                    tagEndInText(text, parser)
                                    if (text != null) {
                                        group.children.add(text)
                                    }
                                    text = null
                                }
                                "editor" -> {
                                    if (editor != null) {
                                        group.children.add(editor)
                                    }
                                    editor = null
                                }
                            }
                        } else {
                            when (parser.name) {
                                "page" -> {
                                    tagEndInPage(page, parser)
                                    if (page != null) {
                                        mainList.add(page)
                                    }
                                    page = null
                                }
                                "action" -> {
                                    tagEndInAction(action, parser)
                                    if (action != null) {
                                        mainList.add(action)
                                    }
                                    action = null
                                }
                                "switch" -> {
                                    tagEndInSwitch(switch, parser)
                                    if (switch != null) {
                                        mainList.add(switch)
                                    }
                                    switch = null
                                }
                                "picker" -> {
                                    tagEndInPicker(picker, parser)
                                    if (picker != null) {
                                        mainList.add(picker)
                                    }
                                    picker = null
                                }
                                "text" -> {
                                    tagEndInText(text, parser)
                                    if (text != null) {
                                        mainList.add(text)
                                    }
                                    text = null
                                }
                                "editor" -> {
                                    if (editor != null) {
                                        mainList.add(editor)
                                    }
                                    editor = null
                                }
                            }
                        }
                }
                type = parser.next()// 继续下一个事件
            }

            return mainList
        } catch (ex: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to parse configuration file\n" + ex.message, Toast.LENGTH_LONG).show()
            }
            Log.e("KrConfig Fail！", "" + ex.message)
        }

        return null
    }

    private var actionParamInfos: ArrayList<ActionParamInfo>? = null
    var actionParamInfo: ActionParamInfo? = null
    private fun tagStartInAction(action: ActionNode, parser: XmlPullParser) {
        if ("title" == parser.name) {
            action.title = StringResRef.resolve(context, parser.nextText())
        } else if ("desc" == parser.name) {
            descNode(action, parser)
        } else if ("summary" == parser.name) {
            summaryNode(action, parser)
        } else if ("script" == parser.name || "set" == parser.name || "setstate" == parser.name) {
            action.setState = parser.nextText().trim()
        } else if ("lock" == parser.name || "lock-state" == parser.name) {
            action.lockShell = parser.nextText()
        } else if ("param" == parser.name) {
            if (actionParamInfos == null) {
                actionParamInfos = ArrayList()
            }
            actionParamInfo = ActionParamInfo()
            val actionParamInfo = actionParamInfo!!
            for (i in 0 until parser.attributeCount) {
                val attrName = parser.getAttributeName(i)
                val attrValue = parser.getAttributeValue(i)
                when (attrName) {
                    "name" -> actionParamInfo.name = attrValue
                    "label" -> actionParamInfo.label = attrValue
                    "placeholder" -> actionParamInfo.placeholder = attrValue
                    "title" -> actionParamInfo.title = attrValue
                    "desc" -> actionParamInfo.desc = attrValue
                    "value" -> actionParamInfo.value = attrValue
                    "type" -> actionParamInfo.type = attrValue.lowercase(getDefault()).trim { it <= ' ' }
                    "suffix" -> {
                        val suffix = attrValue.lowercase(getDefault()).trim { it <= ' ' }

                        if (actionParamInfo.mime.isEmpty()) {
                            actionParamInfo.mime = Suffix2Mime().toMime(suffix)
                        }

                        actionParamInfo.suffix = suffix
                    }
                    "mime" -> {
                        actionParamInfo.mime = attrValue.lowercase(getDefault())
                    }
                    "path-home", "home-path", "pathhome" -> {
                        actionParamInfo.pathHome = attrValue.trim { it <= ' ' }
                    }
                    "readonly" -> {
                        val value = attrValue.lowercase(getDefault()).trim { it <= ' ' }
                        actionParamInfo.readonly = (value == "readonly" || value == "true" || value == "1")
                    }
                    "maxlength" -> actionParamInfo.maxLength = Integer.parseInt(attrValue)
                    "min" -> actionParamInfo.min = Integer.parseInt(attrValue)
                    "max" -> actionParamInfo.max = Integer.parseInt(attrValue)
                    "required" -> actionParamInfo.required = attrValue == "true" || attrValue == "1" || attrValue == "required"
                    "value-sh", "value-su" -> {
                        actionParamInfo.valueShell = attrValue
                    }
                    "options-sh", "option-sh", "options-su" -> {
                        if (actionParamInfo.options == null)
                            actionParamInfo.options = ArrayList()
                        actionParamInfo.optionsSh = attrValue
                    }
                    "support", "visible" -> {
                        if (executeResultRoot(context, attrValue) != "1") {
                            actionParamInfo.supported = false
                        }
                    }
                    "multiple" -> {
                        actionParamInfo.multiple = attrValue == "multiple" || attrValue == "true" || attrValue == "1"
                    }
                    "editable" -> {
                        actionParamInfo.editable = attrValue == "editable" || attrValue == "true" || attrValue == "1"
                    }
                    "separator" -> {
                        actionParamInfo.separator = attrValue
                    }
                    "depend-on", "depend" -> {
                        actionParamInfo.dependOn = attrValue
                    }
                    "depend-value" -> {
                        actionParamInfo.dependValue = attrValue
                    }
                    "depend-mode" -> {
                        actionParamInfo.dependMode = attrValue
                    }
                    "depend-logic", "depend-priority" -> {
                        actionParamInfo.dependLogic = attrValue
                    }
                    "depend-default" -> {
                        actionParamInfo.dependDefault = attrValue
                    }
                    "depend-initial", "depend-initial-state" -> {
                        actionParamInfo.dependInitialState = attrValue
                    }
                    "depend-negate" -> {
                        actionParamInfo.dependNegate = attrValue == "true" || attrValue == "1" || attrValue == "negate"
                    }
                    "depend-threshold" -> {
                        actionParamInfo.dependThreshold = attrValue.toIntOrNull() ?: -1
                    }
                    "depend-include-hidden" -> {
                        actionParamInfo.dependIncludeHidden = attrValue == "true" || attrValue == "1"
                    }
                    "depend-cascade" -> {
                        actionParamInfo.dependCascade = !(attrValue == "false" || attrValue == "0")
                    }
                    "depend-onchange", "depend-on-change", "depend-callback" -> {
                        actionParamInfo.dependOnChangeCallback = attrValue
                    }
                    "depend-readonly" -> {
                        actionParamInfo.dependReadonly = attrValue == "true" || attrValue == "1"
                    }
                }
            }
            if (actionParamInfo.supported && actionParamInfo.name != null && actionParamInfo.name!!.isNotEmpty()) {
                actionParamInfos!!.add(actionParamInfo)
            }
        } else if (actionParamInfo != null && "option" == parser.name) {
            val actionParamInfo = actionParamInfo!!
            if (actionParamInfo.options == null) {
                actionParamInfo.options = ArrayList()
            }
            val option = SelectItem()
            for (i in 0 until parser.attributeCount) {
                val attrName = parser.getAttributeName(i)
                if (attrName == "val" || attrName == "value") {
                    option.value = parser.getAttributeValue(i)
                }
            }
            option.title = StringResRef.resolve(context, parser.nextText())
            if (option.value == null)
                option.value = option.title
            actionParamInfo.options!!.add(option)
        } else if ("resource" == parser.name) {
            resourceNode(parser)
        }
    }

    private fun tagEndInPage(page: PageNode?, parser: XmlPullParser) {
    }

    private fun tagEndInAction(action: ActionNode?, parser: XmlPullParser) {
        if (action != null) {
            if (action.setState == null)
                action.setState = ""
            action.params = actionParamInfos

            actionParamInfos = null
        }
    }

    private fun tagStartInPage(node: PageNode, parser: XmlPullParser) {
        when (parser.name) {
            "title" -> node.title = StringResRef.resolve(context, parser.nextText())
            "desc" -> descNode(node, parser)
            "summary" -> summaryNode(node, parser)
            "resource" -> resourceNode(parser)
            "html" -> node.onlineHtmlPage = parser.nextText()
            "config" -> node.pageConfigPath = parser.nextText()
            "handler-sh", "handler", "set", "getstate", "script" -> node.pageHandlerSh = parser.nextText()
            "lock", "lock-state" -> node.lockShell = parser.nextText()
            "option", "page-option", "menu", "menu-item" -> {
                val option = runnableNode(PageMenuOption(pageConfigAbsPath), parser) as PageMenuOption?
                if (option != null) {
                    for (i in 0 until parser.attributeCount) {
                        when (parser.getAttributeName(i)) {
                            "type" -> {
                                option.type = parser.getAttributeValue(i)
                            }
                            "style" -> {
                                option.isFab = parser.getAttributeValue(i) == "fab"
                            }
                            "suffix" -> {
                                val suffix = parser.getAttributeValue(i).lowercase(getDefault()).trim { it <= ' ' }

                                if (option.mime.isEmpty()) {
                                    option.mime = Suffix2Mime().toMime(suffix)
                                }

                                option.suffix = suffix
                            }
                            "mime" -> {
                                option.mime = parser.getAttributeValue(i).lowercase(getDefault())
                            }
                            "path-home", "home-path", "pathhome" -> {
                                option.pathHome = parser.getAttributeValue(i).trim { it <= ' ' }
                            }
                            "multiple" -> {
                                val attrValue = parser.getAttributeValue(i)
                                option.multiple = attrValue == "multiple" || attrValue == "true" || attrValue == "1"
                            }
                            "box", "visible", "check" -> {
                                option.checkedSh = parser.getAttributeValue(i)
                            }
                            "silent", "hidden" -> {
                                // Khi click, chạy script ẩn ở nền, không hiện dialog log cho người dùng thấy.
                                val attrValue = parser.getAttributeValue(i)
                                option.silent = attrValue.isEmpty() || attrValue == "silent" || attrValue == "hidden" || attrValue == "true" || attrValue == "1"
                            }
                            // Cho phép menu item mở giống 1 "page" (như 1 dòng bình thường trong danh sách)
                            // thay vì chạy pageHandlerSh - ưu tiên xử lý giống hệt onPageClick()/OpenPageHelper:
                            // link > activity > html/config-sh/config
                            "link", "href" -> option.link = parser.getAttributeValue(i)
                            "activity", "a", "intent" -> option.activity = parser.getAttributeValue(i)
                            "html" -> option.onlineHtmlPage = parser.getAttributeValue(i)
                            "config" -> option.pageConfigPath = parser.getAttributeValue(i)
                            "config-sh" -> option.pageConfigSh = parser.getAttributeValue(i)
                        }
                    }
                    option.title = StringResRef.resolve(context, parser.nextText())
                    if (option.key.isEmpty()) {
                        option.key = option.title
                    }

                    if (node.pageMenuOptions == null) {
                        node.pageMenuOptions = ArrayList()
                    }
                    node.pageMenuOptions?.add(option)
                }
            }
        }
    }

    private fun tagStartInSwitch(switchNode: SwitchNode, parser: XmlPullParser) {
        when (parser.name) {
            "title" -> switchNode.title = StringResRef.resolve(context, parser.nextText())
            "desc" -> descNode(switchNode, parser)
            "summary" -> summaryNode(switchNode, parser)
            "get", "getstate" -> switchNode.getState = parser.nextText()
            "set", "setstate" -> switchNode.setState = parser.nextText()
            "resource" -> resourceNode(parser)
            "lock", "lock-state" -> switchNode.lockShell = parser.nextText()
        }
    }

    private fun groupNode(parser: XmlPullParser): GroupNode {
        val groupInfo = GroupNode(pageConfigAbsPath)
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            val attrValue = parser.getAttributeValue(i)
            when (attrName) {
                "key", "index", "id" -> groupInfo.key = attrValue.trim()
                "title" -> groupInfo.title = StringResRef.resolve(context, attrValue)
                "support", "visible" -> groupInfo.supported = executeResultRoot(context, attrValue) == "1"
            }
        }
        return groupInfo
    }

    // 通常指 page、action、switch、picker这种，可以点击的节点
    private fun clickbleNode(clickableNode: ClickableNode, parser: XmlPullParser): ClickableNode? {
        return (mainNode(clickableNode, parser) as ClickableNode?)?.apply {
            for (i in 0 until parser.attributeCount) {
                val attrValue = parser.getAttributeValue(i)
                when (parser.getAttributeName(i)) {
                    "lock", "lock-state", "locked" -> locked = (attrValue == "1" || attrValue == "true" || attrValue == "locked")
                    "min-sdk", "sdk-min" -> minSdkVersion = attrValue.trim().toInt()
                    "max-sdk", "sdk-max" -> maxSdkVersion = attrValue.trim().toInt()
                    "target-sdk", "sdk-target" -> targetSdkVersion = attrValue.trim().toInt()
                    "icon", "icon-path" -> iconPath = attrValue.trim()
                    "logo", "logo-path" -> logoPath = attrValue.trim()
                    "photo", "photo-path" -> photoPath = attrValue.trim()
                    "bg", "bg-path" -> bgPath = attrValue.trim()
                    "allow-shortcut" -> allowShortcut = attrValue == "allow" || attrValue == "allow-shortcut" || attrValue == "true" || attrValue == "1"
                }
            }
            if (key.isNotEmpty() && key.startsWith("@") && allowShortcut == null) {
                allowShortcut = false
            }
        }
    }

    // 通常指 action、switch、picker这种，点击后需要执行脚本的节点
    private fun runnableNode(node: RunnableNode, parser: XmlPullParser): RunnableNode? {
        val clickableNode = clickbleNode(node, parser) as RunnableNode?
        if (clickableNode != null) {
            for (i in 0 until parser.attributeCount) {
                val attrValue = parser.getAttributeValue(i)
                when (parser.getAttributeName(i)) {
                    "confirm" -> clickableNode.confirm = (attrValue == "confirm" || attrValue == "true" || attrValue == "1")
                    "warn", "warning" -> {
                        clickableNode.warning = attrValue
                    }
                    "auto-off", "auto-close" -> clickableNode.autoOff = (attrValue == "auto-close" || attrValue == "auto-off" || attrValue == "true" || attrValue == "1")
                    "auto-finish" -> clickableNode.autoFinish = (attrValue == "auto-finish" || attrValue == "true" || attrValue == "1")
                    "auto-kill" -> clickableNode.autoKill = (attrValue == "auto-kill" || attrValue == "true" || attrValue == "1")
                    "auto-restart" -> clickableNode.autoRestart = (attrValue == "auto-restart" || attrValue == "true" || attrValue == "1")
                    "interruptible", "interruptable" -> clickableNode.interruptable = (
                            attrValue.isEmpty() || attrValue == "interruptable" || attrValue == "interruptable" || attrValue == "true" || attrValue == "1")
                    "need-input", "needs-input", "require-input" -> clickableNode.needInput = (
                            attrValue.isEmpty() || attrValue == "need-input" || attrValue == "true" || attrValue == "1")
                    "reload-page" -> {
                        if (attrValue == "reload-page" || attrValue == "true" || attrValue == "1") {
                            clickableNode.reloadPage = true
                        }
                    }
                    "reload" -> {
                        if (attrValue == "reload" || attrValue == "true" || attrValue == "1") {
                            clickableNode.reloadPage = true
                        } else if (attrValue.isNotEmpty()) {
                            clickableNode.updateBlocks = attrValue.split(",").map { it.trim() }.dropLastWhile { it.isEmpty() }.toTypedArray()
                        }
                    }
                    "shell" -> {
                        clickableNode.shell = attrValue
                    }
                    "bg-task", "background-task", "async-task" -> {
                        if (attrValue == "async-task" || attrValue == "async" || attrValue == "bg-task" || attrValue == "background" || attrValue == "background-task" || attrValue == "true" || attrValue == "1") {
                            clickableNode.shell = RunnableNode.shellModeBgTask
                        }
                    }
                }
            }
        }

        return clickableNode
    }

    private fun mainNode(nodeInfoBase: NodeInfoBase, parser: XmlPullParser): NodeInfoBase? {
        for (i in 0 until parser.attributeCount) {
            val attrValue = parser.getAttributeValue(i)
            when (parser.getAttributeName(i)) {
                "key", "index", "id" -> nodeInfoBase.key = attrValue.trim()
                "title" -> nodeInfoBase.title = attrValue
                "desc" -> nodeInfoBase.desc = attrValue
                "support", "visible" -> {
                    if (executeResultRoot(context, attrValue) != "1") {
                        return null
                    }
                }
                "desc-sh" -> {
                    nodeInfoBase.descSh = parser.getAttributeValue(i)
                    nodeInfoBase.desc = executeResultRoot(context, nodeInfoBase.descSh)
                }
                "summary" -> {
                    nodeInfoBase.summary = parser.getAttributeValue(i)
                }
                "summary-sh" -> {
                    nodeInfoBase.summarySh = parser.getAttributeValue(i)
                    nodeInfoBase.summary = executeResultRoot(context, nodeInfoBase.summarySh)
                }
            }
        }
        return nodeInfoBase
    }

    // TODO: 整理Title和Desc
    // TODO: 整理ReloadPage
    private fun pageNode(page: PageNode, parser: XmlPullParser): PageNode {
        for (attrIndex in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(attrIndex)
            val attrValue = parser.getAttributeValue(attrIndex)
            when (attrName) {
                "config" -> page.pageConfigPath = attrValue
                "html" -> page.onlineHtmlPage = attrValue
                "before-load", "before-read" -> page.beforeRead = attrValue
                "after-load", "after-read" -> page.afterRead = attrValue
                "load-ok", "load-success" -> page.loadSuccess = attrValue
                "load-fail", "load-error" -> page.loadFail = attrValue
                "config-sh" -> page.pageConfigSh = attrValue
                "link", "href" -> page.link = attrValue
                "activity", "a", "intent" -> page.activity = attrValue
                "option-sh", "option-su", "options-sh" -> page.pageMenuOptionsSh = attrValue
                "handler-sh", "handler", "set", "getstate", "script" -> page.pageHandlerSh = attrValue
            }
        }
        return page
    }

    private fun pickerNode(pickerNode: PickerNode, parser: XmlPullParser) {
        for (attrIndex in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(attrIndex)
            val attrValue = parser.getAttributeValue(attrIndex)
            when (attrName) {
                "option-sh", "options-sh", "options-su" -> {
                    if (pickerNode.options == null)
                        pickerNode.options = ArrayList()
                    pickerNode.optionsSh = attrValue
                }
                "multiple" -> {
                    pickerNode.multiple = attrValue == "multiple" || attrValue == "true" || attrValue == "1"
                }
                "separator" -> {
                    pickerNode.separator = attrValue
                }
            }
        }
    }

    // Đọc thuộc tính riêng của thẻ <editor file="" title="" desc="" wrap="" />
    private fun editorNode(editor: EditorNode, parser: XmlPullParser): EditorNode {
        for (attrIndex in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(attrIndex)
            val attrValue = parser.getAttributeValue(attrIndex)
            when (attrName) {
                "file", "path" -> editor.file = attrValue.trim()
                "wrap" -> editor.wrap = !(attrValue == "0" || attrValue == "false" || attrValue == "off" || attrValue == "no-wrap")
                "placeholder" -> editor.placeholder = attrValue
            }
        }
        return editor
    }

    private fun descNode(nodeInfoBase: NodeInfoBase, parser: XmlPullParser) {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            if (attrName == "su" || attrName == "sh" || attrName == "desc-sh") {
                nodeInfoBase.descSh = parser.getAttributeValue(i)
                nodeInfoBase.desc = executeResultRoot(context, nodeInfoBase.descSh)
            }
        }
        if (nodeInfoBase.desc.isEmpty())
            nodeInfoBase.desc = StringResRef.resolve(context, parser.nextText())
    }

    private fun summaryNode(nodeInfoBase: NodeInfoBase, parser: XmlPullParser) {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i)
            if (attrName == "su" || attrName == "sh" || attrName == "summary-sh") {
                nodeInfoBase.summarySh = parser.getAttributeValue(i)
                nodeInfoBase.summary = executeResultRoot(context, nodeInfoBase.summarySh)
            }
        }
        if (nodeInfoBase.summary.isEmpty())
            nodeInfoBase.summary = StringResRef.resolve(context, parser.nextText())
    }

    private fun resourceNode(parser: XmlPullParser) {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i) == "file") {
                val file = parser.getAttributeValue(i).trim()
                ExtractAssets(context).extractResource(file)
            } else if (parser.getAttributeName(i) == "dir") {
                val file = parser.getAttributeValue(i).trim()
                ExtractAssets(context).extractResources(file)
            }
        }
    }

    private fun tagEndInSwitch(switchNode: SwitchNode?, parser: XmlPullParser) {
        if (switchNode != null) {
            val shellResult = executeResultRoot(context, switchNode.getState)
            switchNode.checked = shellResult != "error" && (shellResult == "1" || shellResult.lowercase(
                getDefault()
            ) == "true")
            if (switchNode.setState == null) {
                switchNode.setState = ""
            }
        }
    }

    private fun tagStartInText(textNode: TextNode, parser: XmlPullParser) {
        when (parser.name) {
            "title" -> {
                textNode.title = StringResRef.resolve(context, parser.nextText())
            }
            "desc" -> {
                descNode(textNode, parser)
            }
            "summary" -> {
                summaryNode(textNode, parser)
            }
            "slice" -> {
                rowNode(textNode, parser)
            }
            "resource" -> {
                resourceNode(parser)
            }
        }
    }

    private fun rowNode(textNode: TextNode, parser: XmlPullParser) {
        val textRow = TextNode.TextRow()
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i).lowercase(getDefault())
            val attrValue = parser.getAttributeValue(i)
            try {
                when (attrName) {
                    "bold", "b" -> textRow.bold = (attrValue == "1" || attrValue == "true" || attrValue == "bold")
                    "italic", "i" -> textRow.italic = (attrValue == "1" || attrValue == "true" || attrValue == "italic")
                    "underline", "u" -> textRow.underline = (attrValue == "1" || attrValue == "true" || attrValue == "underline")
                    "foreground", "color" -> textRow.color = attrValue.toColorInt()
                    "bg", "background", "bgcolor" -> textRow.bgColor = attrValue.toColorInt()
                    "size" -> textRow.size = attrValue.toInt()
                    "break" -> textRow.breakRow = (attrValue == "1" || attrValue == "true" || attrValue == "break")
                    "link", "href" -> textRow.link = attrValue
                    "activity", "a", "intent" -> textRow.activity = attrValue
                    "photo", "photo-path" -> textRow.photo = attrValue.trim()
                    "script", "run" -> {
                        textRow.onClickScript = attrValue
                    }
                    "sh" -> {
                        textRow.dynamicTextSh = attrValue
                    }
                    "align" -> {
                        when (attrValue) {
                            "opposite" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                textRow.align = Layout.Alignment.ALIGN_OPPOSITE
                            }

                            "center" -> textRow.align = Layout.Alignment.ALIGN_CENTER
                            "normal" -> textRow.align = Layout.Alignment.ALIGN_NORMAL
                        }
                    }
                }
            } catch (ex: Exception) {
            }
        }
        textRow.text = StringResRef.resolve(context, parser.nextText())
        textNode.rows.add(textRow)
    }

    private fun tagStartInPicker(pickerNode: PickerNode, parser: XmlPullParser) {
        when (parser.name) {
            "title" -> {
                pickerNode.title = StringResRef.resolve(context, parser.nextText())
            }
            "desc" -> {
                descNode(pickerNode, parser)
            }
            "summary" -> {
                summaryNode(pickerNode, parser)
            }
            "option" -> {
                if (pickerNode.options == null) {
                    pickerNode.options = ArrayList()
                }
                val option = SelectItem()
                for (i in 0 until parser.attributeCount) {
                    val attrName = parser.getAttributeName(i)
                    if (attrName == "val" || attrName == "value") {
                        option.value = parser.getAttributeValue(i)
                    }
                }
                option.title = StringResRef.resolve(context, parser.nextText())
                if (option.value == null)
                    option.value = option.title
                pickerNode.options!!.add(option)
            }
            "getstate", "get" -> {
                pickerNode.getState = parser.nextText()
            }
            "setstate", "set" -> {
                pickerNode.setState = parser.nextText()
            }
            "resource" -> {
                resourceNode(parser)
            }
            "lock", "lock-state" -> {
                pickerNode.lockShell = parser.nextText()
            }
        }
    }

    private fun tagEndInPicker(pickerNode: PickerNode?, parser: XmlPullParser) {
        if (pickerNode != null) {
            if (pickerNode.getState == null) {
                pickerNode.getState = ""
            } else {
                val shellResult = executeResultRoot(context, "" + pickerNode.getState)
                pickerNode.value = shellResult
            }
            if (pickerNode.setState == null) {
                pickerNode.setState = ""
            }
        }
    }

    private fun tagEndInText(textNode: TextNode?, parser: XmlPullParser) {
    }

    private var vitualRootNode: NodeInfoBase? = null
    private fun executeResultRoot(context: Context, scriptIn: String): String {
        if (vitualRootNode == null) {
            vitualRootNode = NodeInfoBase(pageConfigAbsPath)
        }

        return ScriptEnvironmen.executeResultRoot(context, scriptIn, vitualRootNode)
    }

    // =====================================================================================
    // Hỗ trợ đọc cấu hình dạng TOML (song song với XML, không thay đổi bất kỳ hành vi XML nào)
    //
    // Tên loại node = tên bảng TOML (không cần field `type` riêng):
    //   group | page | action | switch | picker | text | editor | resource
    //
    // Khuyến nghị: LUÔN dùng 2 NGOẶC [[ten]] cho mọi mục, kể cả khi hiện tại chỉ có 1
    // mục loại đó - để tránh về sau lỡ thêm mục thứ 2 cùng tên mà quên đổi ngoặc (TOML
    // KHÔNG cho phép trộn [ten] và [[ten]] cho cùng 1 khoá ở cùng vị trí, sẽ lỗi parse).
    // Ví dụ SAI: khai báo [group] rồi sau đó lại có thêm [[group]] khác trong cùng file.
    //
    // Trình đọc (tomlEntries) vẫn CHẤP NHẬN cả dạng 1 ngoặc [ten] (bảng đơn) cho những
    // trường hợp chắc chắn chỉ có đúng 1 mục, nhưng để an toàn nên ưu tiên 2 ngoặc.
    //
    // Các mục nằm "bên trong" một group (con của nó) khai báo bằng đường dẫn lồng:
    //   [[group.action]], [[group.page]], [[group.text]] ...
    //
    // Giới hạn cần biết: TOML không lưu được thứ tự XEN KẼ giữa các bảng KHÁC TÊN
    // (vd 1 action rồi tới 1 page rồi lại 1 action nữa) - mỗi tên gom thành 1 danh sách
    // riêng. Mặc định các loại được sắp theo thứ tự: group, text, switch, picker, action,
    // page, editor, resource; nếu cần chỉ định thứ tự chính xác, thêm field `order = <số>`
    // (số nhỏ hơn hiện trước) vào từng mục.
    //
    // Ví dụ:
    //   [[group]]
    //   title = "Nhóm 1"
    //
    //     [[group.action]]
    //     title = "Xoá cache"
    //     confirm = true
    //     script = "rm -rf /cache/*"
    //
    //       [[group.action.params]]
    //       name = "path"
    //       title = "Đường dẫn"
    //       type = "text"
    //
    //     [[group.page]]
    //     title = "Xem log"
    //     config = "log_page.toml"
    // =====================================================================================

    private val tomlNodeTypeOrder = listOf("group", "text", "switch", "picker", "action", "page", "editor", "resource")

    private fun tomlGet(table: TomlTable, vararg keys: String): String? {
        for (key in keys) {
            if (table.contains(key)) {
                val value = table.get(key) ?: continue
                return value.toString()
            }
        }
        return null
    }

    private fun tomlTruthy(raw: String?, vararg extraTruthyValues: String): Boolean {
        if (raw == null) return false
        val v = raw.lowercase(getDefault()).trim()
        return v == "1" || v == "true" || extraTruthyValues.any { it == v }
    }

    // Đọc 1 khoá `key` trong bảng `parent`, chấp nhận cả 2 dạng: bảng đơn [key] (1 ngoặc)
    // hoặc mảng bảng [[key]] (2 ngoặc). Luôn trả về danh sách để xử lý đồng nhất.
    private fun tomlEntries(parent: TomlTable, key: String): List<TomlTable> {
        return when {
            parent.isArray(key) -> {
                val arr = parent.getArray(key) ?: return emptyList()
                (0 until arr.size()).map { arr.getTable(it) }
            }
            parent.isTable(key) -> listOf(parent.getTable(key)!!)
            else -> emptyList()
        }
    }

    private fun readConfigToml(rawText: String): ArrayList<NodeInfoBase>? {
        return try {
            val result = Toml.parse(rawText)
            if (result.hasErrors()) {
                val message = result.errors().joinToString("\n") { it.toString() }
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to parse configuration file (toml)\n$message", Toast.LENGTH_LONG).show()
                }
                Log.e("KrConfig Fail！", message)
                return null
            }
            tomlChildren(result)
        } catch (ex: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to parse configuration file\n" + ex.message, Toast.LENGTH_LONG).show()
            }
            Log.e("KrConfig Fail！", "" + ex.message)
            null
        }
    }

    // Gom toàn bộ node con của 1 bảng (root, hoặc bảng của 1 group), theo đúng thứ tự
    // mặc định (tomlNodeTypeOrder), trừ khi từng mục có field `order` để chỉ định vị trí.
    private fun tomlChildren(parent: TomlTable): ArrayList<NodeInfoBase> {
        data class Entry(val order: Double, val seq: Int, val node: NodeInfoBase)

        val entries = ArrayList<Entry>()
        var seq = 0
        for ((typeIndex, type) in tomlNodeTypeOrder.withIndex()) {
            val defaultBase = typeIndex * 100000.0
            for (table in tomlEntries(parent, type)) {
                seq++
                val node = tomlBuildNode(type, table) ?: continue
                val explicitOrder = tomlGet(table, "order")?.trim()?.toDoubleOrNull()
                entries.add(Entry(explicitOrder ?: (defaultBase + seq), seq, node))
            }
        }
        return ArrayList(entries.sortedWith(compareBy({ it.order }, { it.seq })).map { it.node })
    }

    private fun tomlBuildNode(type: String, table: TomlTable): NodeInfoBase? {
        return when (type) {
            "group" -> {
                val group = groupNodeToml(table)
                if (!group.supported) {
                    null
                } else {
                    group.children.addAll(tomlChildren(table))
                    group
                }
            }
            "page" -> pageNodeToml(table)
            "action" -> actionNodeToml(table)
            "switch" -> switchNodeToml(table)
            "picker" -> pickerNodeToml(table)
            "text" -> textNodeToml(table)
            "editor" -> editorNodeToml(table)
            "resource" -> {
                resourceNodeToml(table)
                null
            }
            else -> null
        }
    }

    private fun mainNodeToml(nodeInfoBase: NodeInfoBase, table: TomlTable): NodeInfoBase? {
        tomlGet(table, "support", "visible")?.let {
            if (executeResultRoot(context, it) != "1") return null
        }
        tomlGet(table, "key", "index", "id")?.let { nodeInfoBase.key = it.trim() }
        tomlGet(table, "title")?.let { nodeInfoBase.title = StringResRef.resolve(context, it) }
        tomlGet(table, "desc-sh")?.let {
            nodeInfoBase.descSh = it
            nodeInfoBase.desc = executeResultRoot(context, it)
        }
        if (nodeInfoBase.desc.isEmpty()) {
            tomlGet(table, "desc")?.let { nodeInfoBase.desc = StringResRef.resolve(context, it) }
        }
        tomlGet(table, "summary-sh")?.let {
            nodeInfoBase.summarySh = it
            nodeInfoBase.summary = executeResultRoot(context, it)
        }
        if (nodeInfoBase.summary.isEmpty()) {
            tomlGet(table, "summary")?.let { nodeInfoBase.summary = StringResRef.resolve(context, it) }
        }
        return nodeInfoBase
    }

    private fun clickableNodeToml(node: ClickableNode, table: TomlTable): ClickableNode? {
        return (mainNodeToml(node, table) as ClickableNode?)?.apply {
            tomlGet(table, "lock", "lock-state", "locked")?.let { locked = tomlTruthy(it, "locked") }
            tomlGet(table, "min-sdk", "sdk-min")?.let { minSdkVersion = it.trim().toIntOrNull() ?: minSdkVersion }
            tomlGet(table, "max-sdk", "sdk-max")?.let { maxSdkVersion = it.trim().toIntOrNull() ?: maxSdkVersion }
            tomlGet(table, "target-sdk", "sdk-target")?.let { targetSdkVersion = it.trim().toIntOrNull() ?: targetSdkVersion }
            tomlGet(table, "icon", "icon-path")?.let { iconPath = it.trim() }
            tomlGet(table, "logo", "logo-path")?.let { logoPath = it.trim() }
            tomlGet(table, "photo", "photo-path")?.let { photoPath = it.trim() }
            tomlGet(table, "bg", "bg-path")?.let { bgPath = it.trim() }
            tomlGet(table, "allow-shortcut")?.let { allowShortcut = tomlTruthy(it, "allow", "allow-shortcut") }
            if (key.isNotEmpty() && key.startsWith("@") && allowShortcut == null) {
                allowShortcut = false
            }
        }
    }

    private fun runnableNodeToml(node: RunnableNode, table: TomlTable): RunnableNode? {
        return (clickableNodeToml(node, table) as RunnableNode?)?.apply {
            tomlGet(table, "confirm")?.let { confirm = tomlTruthy(it, "confirm") }
            tomlGet(table, "warn", "warning")?.let { warning = it }
            tomlGet(table, "auto-off", "auto-close")?.let { autoOff = tomlTruthy(it, "auto-close", "auto-off") }
            tomlGet(table, "auto-finish")?.let { autoFinish = tomlTruthy(it, "auto-finish") }
            tomlGet(table, "auto-kill")?.let { autoKill = tomlTruthy(it, "auto-kill") }
            tomlGet(table, "auto-restart")?.let { autoRestart = tomlTruthy(it, "auto-restart") }
            tomlGet(table, "interruptible", "interruptable")?.let {
                interruptable = it.isEmpty() || tomlTruthy(it, "interruptable")
            }
            tomlGet(table, "need-input", "needs-input", "require-input")?.let {
                needInput = it.isEmpty() || tomlTruthy(it, "need-input")
            }
            tomlGet(table, "reload-page")?.let { if (tomlTruthy(it, "reload-page")) reloadPage = true }
            tomlGet(table, "reload")?.let {
                if (tomlTruthy(it, "reload")) {
                    reloadPage = true
                } else if (it.isNotEmpty()) {
                    updateBlocks = it.split(",").map { s -> s.trim() }.dropLastWhile { s -> s.isEmpty() }.toTypedArray()
                }
            }
            tomlGet(table, "shell")?.let { shell = it }
            tomlGet(table, "bg-task", "background-task", "async-task")?.let {
                if (tomlTruthy(it, "async-task", "async", "bg-task", "background", "background-task")) {
                    shell = RunnableNode.shellModeBgTask
                }
            }
        }
    }

    private fun groupNodeToml(table: TomlTable): GroupNode {
        val group = GroupNode(pageConfigAbsPath)
        tomlGet(table, "key", "index", "id")?.let { group.key = it.trim() }
        tomlGet(table, "title")?.let { group.title = StringResRef.resolve(context, it) }
        tomlGet(table, "support", "visible")?.let { group.supported = executeResultRoot(context, it) == "1" }
        return group
    }

    private fun pageNodeToml(table: TomlTable): PageNode? {
        val page = clickableNodeToml(PageNode(pageConfigAbsPath), table) as PageNode? ?: return null
        tomlGet(table, "config")?.let { page.pageConfigPath = it }
        tomlGet(table, "html")?.let { page.onlineHtmlPage = it }
        tomlGet(table, "before-load", "before-read")?.let { page.beforeRead = it }
        tomlGet(table, "after-load", "after-read")?.let { page.afterRead = it }
        tomlGet(table, "load-ok", "load-success")?.let { page.loadSuccess = it }
        tomlGet(table, "load-fail", "load-error")?.let { page.loadFail = it }
        tomlGet(table, "config-sh")?.let { page.pageConfigSh = it }
        tomlGet(table, "link", "href")?.let { page.link = it }
        tomlGet(table, "activity", "a", "intent")?.let { page.activity = it }
        tomlGet(table, "option-sh", "option-su", "options-sh")?.let { page.pageMenuOptionsSh = it }
        tomlGet(table, "handler-sh", "handler", "set", "getstate", "script")?.let { page.pageHandlerSh = it }
        tomlGet(table, "lock", "lock-state")?.let { page.lockShell = it }

        val pageOptions = tomlEntries(table, "options")
        if (pageOptions.isNotEmpty()) {
            if (page.pageMenuOptions == null) page.pageMenuOptions = ArrayList()
            for (optTable in pageOptions) {
                pageMenuOptionToml(optTable)?.let { page.pageMenuOptions!!.add(it) }
            }
        }
        resourceNodeToml(table)
        return page
    }

    private fun pageMenuOptionToml(table: TomlTable): PageMenuOption? {
        val option = runnableNodeToml(PageMenuOption(pageConfigAbsPath), table) as PageMenuOption? ?: return null
        tomlGet(table, "type")?.let { option.type = it }
        tomlGet(table, "style")?.let { option.isFab = it == "fab" }
        tomlGet(table, "suffix")?.let {
            val suffix = it.lowercase(getDefault()).trim()
            if (option.mime.isEmpty()) option.mime = Suffix2Mime().toMime(suffix)
            option.suffix = suffix
        }
        tomlGet(table, "mime")?.let { option.mime = it.lowercase(getDefault()) }
        tomlGet(table, "path-home", "home-path", "pathhome")?.let { option.pathHome = it.trim() }
        tomlGet(table, "multiple")?.let { option.multiple = tomlTruthy(it, "multiple") }
        tomlGet(table, "box", "visible", "check")?.let { option.checkedSh = it }
        tomlGet(table, "silent", "hidden")?.let { option.silent = it.isEmpty() || tomlTruthy(it, "silent", "hidden") }
        tomlGet(table, "link", "href")?.let { option.link = it }
        tomlGet(table, "activity", "a", "intent")?.let { option.activity = it }
        tomlGet(table, "html")?.let { option.onlineHtmlPage = it }
        tomlGet(table, "config")?.let { option.pageConfigPath = it }
        tomlGet(table, "config-sh")?.let { option.pageConfigSh = it }
        tomlGet(table, "title", "text")?.let { option.title = StringResRef.resolve(context, it) }
        if (option.key.isEmpty()) option.key = option.title
        return option
    }

    private fun switchNodeToml(table: TomlTable): SwitchNode? {
        val switchNode = runnableNodeToml(SwitchNode(pageConfigAbsPath), table) as SwitchNode? ?: return null
        tomlGet(table, "get", "getstate")?.let { switchNode.getState = it }
        tomlGet(table, "set", "setstate")?.let { switchNode.setState = it }
        tomlGet(table, "lock", "lock-state")?.let { switchNode.lockShell = it }
        resourceNodeToml(table)

        val shellResult = executeResultRoot(context, switchNode.getState)
        switchNode.checked = shellResult != "error" && (shellResult == "1" || shellResult.lowercase(getDefault()) == "true")
        if (switchNode.setState == null) {
            switchNode.setState = ""
        }
        return switchNode
    }

    private fun pickerNodeToml(table: TomlTable): PickerNode? {
        val picker = runnableNodeToml(PickerNode(pageConfigAbsPath), table) as PickerNode? ?: return null
        tomlGet(table, "option-sh", "options-sh", "options-su")?.let {
            if (picker.options == null) picker.options = ArrayList()
            picker.optionsSh = it
        }
        tomlGet(table, "multiple")?.let { picker.multiple = tomlTruthy(it, "multiple") }
        tomlGet(table, "separator")?.let { picker.separator = it }
        tomlGet(table, "get", "getstate")?.let { picker.getState = it }
        tomlGet(table, "set", "setstate")?.let { picker.setState = it }
        tomlGet(table, "lock", "lock-state")?.let { picker.lockShell = it }

        val pickerOptions = tomlEntries(table, "options")
        if (pickerOptions.isNotEmpty()) {
            if (picker.options == null) picker.options = ArrayList()
            for (optTable in pickerOptions) {
                val item = SelectItem()
                tomlGet(optTable, "val", "value")?.let { item.value = it }
                tomlGet(optTable, "title", "text")?.let { item.title = StringResRef.resolve(context, it) }
                if (item.value == null) item.value = item.title
                picker.options!!.add(item)
            }
        }
        resourceNodeToml(table)

        if (picker.getState == null) {
            picker.getState = ""
        } else {
            picker.value = executeResultRoot(context, "" + picker.getState)
        }
        if (picker.setState == null) picker.setState = ""
        return picker
    }

    private fun actionNodeToml(table: TomlTable): ActionNode? {
        val action = runnableNodeToml(ActionNode(pageConfigAbsPath), table) as ActionNode? ?: return null
        tomlGet(table, "script", "set", "setstate")?.let { action.setState = it.trim() }
        tomlGet(table, "lock", "lock-state")?.let { action.lockShell = it }
        if (action.setState == null) action.setState = ""

        val paramTables = tomlEntries(table, "params")
        if (paramTables.isNotEmpty()) {
            val params = ArrayList<ActionParamInfo>()
            for (paramTable in paramTables) {
                actionParamToml(paramTable)?.let { params.add(it) }
            }
            action.params = params
        }
        resourceNodeToml(table)
        return action
    }

    private fun actionParamToml(table: TomlTable): ActionParamInfo? {
        val p = ActionParamInfo()
        tomlGet(table, "name")?.let { p.name = it }
        tomlGet(table, "label")?.let { p.label = it }
        tomlGet(table, "placeholder")?.let { p.placeholder = it }
        tomlGet(table, "title")?.let { p.title = it }
        tomlGet(table, "desc")?.let { p.desc = it }
        tomlGet(table, "value")?.let { p.value = it }
        tomlGet(table, "type")?.let { p.type = it.lowercase(getDefault()).trim() }
        tomlGet(table, "suffix")?.let {
            val suffix = it.lowercase(getDefault()).trim()
            if (p.mime.isEmpty()) p.mime = Suffix2Mime().toMime(suffix)
            p.suffix = suffix
        }
        tomlGet(table, "mime")?.let { p.mime = it.lowercase(getDefault()) }
        tomlGet(table, "path-home", "home-path", "pathhome")?.let { p.pathHome = it.trim() }
        tomlGet(table, "readonly")?.let { p.readonly = tomlTruthy(it, "readonly") }
        tomlGet(table, "maxlength")?.let { p.maxLength = it.trim().toIntOrNull() ?: p.maxLength }
        tomlGet(table, "min")?.let { p.min = it.trim().toIntOrNull() ?: p.min }
        tomlGet(table, "max")?.let { p.max = it.trim().toIntOrNull() ?: p.max }
        tomlGet(table, "required")?.let { p.required = tomlTruthy(it, "required") }
        tomlGet(table, "value-sh", "value-su")?.let { p.valueShell = it }
        tomlGet(table, "options-sh", "option-sh", "options-su")?.let {
            if (p.options == null) p.options = ArrayList()
            p.optionsSh = it
        }
        tomlGet(table, "support", "visible")?.let {
            if (executeResultRoot(context, it) != "1") p.supported = false
        }
        tomlGet(table, "multiple")?.let { p.multiple = tomlTruthy(it, "multiple") }
        tomlGet(table, "editable")?.let { p.editable = tomlTruthy(it, "editable") }
        tomlGet(table, "separator")?.let { p.separator = it }
        tomlGet(table, "depend-on", "depend")?.let { p.dependOn = it }
        tomlGet(table, "depend-value")?.let { p.dependValue = it }
        tomlGet(table, "depend-mode")?.let { p.dependMode = it }
        tomlGet(table, "depend-logic", "depend-priority")?.let { p.dependLogic = it }
        tomlGet(table, "depend-default")?.let { p.dependDefault = it }
        tomlGet(table, "depend-initial", "depend-initial-state")?.let { p.dependInitialState = it }
        tomlGet(table, "depend-negate")?.let { p.dependNegate = tomlTruthy(it, "negate") }
        tomlGet(table, "depend-threshold")?.let { p.dependThreshold = it.trim().toIntOrNull() ?: -1 }
        tomlGet(table, "depend-include-hidden")?.let { p.dependIncludeHidden = tomlTruthy(it) }
        tomlGet(table, "depend-cascade")?.let { p.dependCascade = !(it == "false" || it == "0") }
        tomlGet(table, "depend-onchange", "depend-on-change", "depend-callback")?.let { p.dependOnChangeCallback = it }
        tomlGet(table, "depend-readonly")?.let { p.dependReadonly = tomlTruthy(it) }

        val paramOptions = tomlEntries(table, "options")
        if (paramOptions.isNotEmpty()) {
            if (p.options == null) p.options = ArrayList()
            for (optTable in paramOptions) {
                val item = SelectItem()
                tomlGet(optTable, "val", "value")?.let { item.value = it }
                tomlGet(optTable, "title", "text")?.let { item.title = StringResRef.resolve(context, it) }
                if (item.value == null) item.value = item.title
                p.options!!.add(item)
            }
        }

        return if (p.supported && !p.name.isNullOrEmpty()) p else null
    }

    private fun textNodeToml(table: TomlTable): TextNode? {
        val text = mainNodeToml(TextNode(pageConfigAbsPath), table) as TextNode? ?: return null
        for (rowTable in tomlEntries(table, "rows")) {
            textRowToml(text, rowTable)
        }
        resourceNodeToml(table)
        return text
    }

    private fun textRowToml(textNode: TextNode, table: TomlTable) {
        val row = TextNode.TextRow()
        tomlGet(table, "bold", "b")?.let { row.bold = tomlTruthy(it, "bold") }
        tomlGet(table, "italic", "i")?.let { row.italic = tomlTruthy(it, "italic") }
        tomlGet(table, "underline", "u")?.let { row.underline = tomlTruthy(it, "underline") }
        tomlGet(table, "foreground", "color")?.let { try { row.color = it.toColorInt() } catch (_: Exception) {} }
        tomlGet(table, "bg", "background", "bgcolor")?.let { try { row.bgColor = it.toColorInt() } catch (_: Exception) {} }
        tomlGet(table, "size")?.let { row.size = it.trim().toIntOrNull() ?: row.size }
        tomlGet(table, "break")?.let { row.breakRow = tomlTruthy(it, "break") }
        tomlGet(table, "link", "href")?.let { row.link = it }
        tomlGet(table, "activity", "a", "intent")?.let { row.activity = it }
        tomlGet(table, "photo", "photo-path")?.let { row.photo = it.trim() }
        tomlGet(table, "script", "run")?.let { row.onClickScript = it }
        tomlGet(table, "sh")?.let { row.dynamicTextSh = it }
        tomlGet(table, "align")?.let {
            when (it) {
                "opposite" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) row.align = Layout.Alignment.ALIGN_OPPOSITE
                "center" -> row.align = Layout.Alignment.ALIGN_CENTER
                "normal" -> row.align = Layout.Alignment.ALIGN_NORMAL
            }
        }
        tomlGet(table, "text")?.let { row.text = StringResRef.resolve(context, it) }
        textNode.rows.add(row)
    }

    private fun editorNodeToml(table: TomlTable): EditorNode? {
        val editor = clickableNodeToml(EditorNode(pageConfigAbsPath), table) as EditorNode? ?: return null
        tomlGet(table, "file", "path")?.let { editor.file = it.trim() }
        tomlGet(table, "wrap")?.let { editor.wrap = !(it == "0" || it == "false" || it == "off" || it == "no-wrap") }
        tomlGet(table, "placeholder")?.let { editor.placeholder = it }
        return editor
    }

    private fun resourceNodeToml(table: TomlTable) {
        tomlGet(table, "resource-file")?.let { ExtractAssets(context).extractResource(it.trim()) }
        tomlGet(table, "resource-dir")?.let { ExtractAssets(context).extractResources(it.trim()) }
        for (resTable in tomlEntries(table, "resources")) {
            tomlGet(resTable, "file")?.let { ExtractAssets(context).extractResource(it.trim()) }
            tomlGet(resTable, "dir")?.let { ExtractAssets(context).extractResources(it.trim()) }
        }
    }
}