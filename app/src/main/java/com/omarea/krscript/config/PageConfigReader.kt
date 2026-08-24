package com.omarea.krscript.config

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.util.Log
import android.widget.Toast
import com.omarea.common.model.SelectItem
import com.omarea.krscript.executor.ExtractAssets
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.*
import java.io.InputStream
import java.util.Locale.getDefault
import androidx.core.graphics.toColorInt
// Thư viện phân tích TOML (cần thêm vào build.gradle, ví dụ: implementation("org.tomlj:tomlj:1.1.1"))
import org.tomlj.Toml
import org.tomlj.TomlArray
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

    constructor(context: Context, pageConfig: String, parentDir: String?) {
        this.context = context
        this.pageConfig = pageConfig
        this.parentDir = parentDir ?: ""
    }

    constructor(context: Context, pageConfigStream: InputStream) {
        this.context = context
        this.pageConfigStream = pageConfigStream
    }

    // onNodeReady (chỉ dùng khi process = true): được gọi ngay sau khi TỪNG mục ở CẤP CAO
    // NHẤT build xong (đúng thứ tự trên xuống theo dòng trong file) - node = null nghĩa là
    // mục đó bị lọc bỏ (support/visible = false...), vẫn báo để bên gọi cập nhật % tiến
    // trình. Truyền null (mặc định) thì hành vi y hệt như cũ: build hết rồi mới trả về.
    fun readConfigXml(onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase>? {
        if (pageConfigStream != null) {
            return readConfigXml(pageConfigStream!!, onNodeReady)
        } else {
            try {
                val pathAnalysis = PathAnalysis(context, parentDir)
                pathAnalysis.parsePath(pageConfig).run {
                    val fileInputStream = this ?: return ArrayList()
                    pageConfigAbsPath = pathAnalysis.getCurrentAbsPath()
                    return readConfigXml(fileInputStream, onNodeReady)
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

    private fun readConfigXml(fileInputStream: InputStream, onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase>? {
        return try {
            val rawText = fileInputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            readConfigToml(rawText, onNodeReady)
        } catch (ex: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to parse configuration file\n" + ex.message, Toast.LENGTH_LONG).show()
            }
            Log.e("KrConfig Fail！", "" + ex.message)
            null
        }
    }

    private var vitualRootNode: NodeInfoBase? = null
    private fun executeResultRoot(context: Context, scriptIn: String): String {
        if (vitualRootNode == null) {
            vitualRootNode = NodeInfoBase(pageConfigAbsPath)
        }

        return ScriptEnvironmen.executeResultRoot(context, scriptIn, vitualRootNode)
    }

    // ========== TỐI ƯU: HÀNG CHỜ getState CỦA MỌI SWITCH/PICKER TRONG TRANG ==========
    // Thay vì mỗi switch/picker tự chạy getState riêng lẻ ngay lúc parse (N mục = N
    // round-trip shell TUẦN TỰ - nguyên nhân gây delay khi menu có từ 3 checkbox trở lên),
    // switchNodeToml()/pickerNodeToml() chỉ ĐĂNG KÝ script vào đây. Toàn bộ được gộp và
    // chạy ĐÚNG 1 LẦN ở cuối readConfigToml() qua resolvePendingStates().
    private val pendingSwitchStates = ArrayList<Pair<SwitchNode, String>>()
    private val pendingPickerStates = ArrayList<Pair<PickerNode, String>>()
    // Tương tự pendingSwitchStates/pendingPickerStates nhưng cho row trong "rows" (text/action).
    // checked: (row, script) - row.checked sẽ được cập nhật sau khi gộp.
    // visible: (danh sách rows chứa nó, row, script) - nếu kết quả gộp = false thì row bị xoá
    // khỏi danh sách sau (row vẫn được thêm tạm vào danh sách lúc parse để giữ đúng thứ tự).
    private val pendingRowCheckedStates = ArrayList<Pair<TextNode.TextRow, String>>()
    private val pendingRowVisibleStates = ArrayList<Triple<ArrayList<TextNode.TextRow>, TextNode.TextRow, String>>()

    private fun resolvePendingStates() {
        if (pendingSwitchStates.isEmpty() && pendingPickerStates.isEmpty() &&
            pendingRowCheckedStates.isEmpty() && pendingRowVisibleStates.isEmpty()) return

        val scripts = LinkedHashMap<String, String>()
        pendingSwitchStates.forEachIndexed { index, pair -> scripts["switch:$index"] = pair.second }
        pendingPickerStates.forEachIndexed { index, pair -> scripts["picker:$index"] = pair.second }
        pendingRowCheckedStates.forEachIndexed { index, pair -> scripts["row-checked:$index"] = pair.second }
        pendingRowVisibleStates.forEachIndexed { index, triple -> scripts["row-visible:$index"] = triple.third }

        if (vitualRootNode == null) {
            vitualRootNode = NodeInfoBase(pageConfigAbsPath)
        }
        val results = ScriptEnvironmen.executeMultipleResultRoot(context, scripts, vitualRootNode)

        pendingSwitchStates.forEachIndexed { index, pair ->
            val shellResult = results["switch:$index"] ?: ""
            pair.first.checked = shellResult != "error" && (shellResult == "1" || shellResult.lowercase(getDefault()) == "true")
        }
        pendingPickerStates.forEachIndexed { index, pair ->
            results["picker:$index"]?.let { pair.first.value = it }
        }
        pendingRowCheckedStates.forEachIndexed { index, pair ->
            val shellResult = results["row-checked:$index"] ?: ""
            pair.first.checked = shellResult.trim() == "1"
        }
        pendingRowVisibleStates.forEachIndexed { index, triple ->
            val shellResult = results["row-visible:$index"] ?: ""
            if (shellResult.trim() != "1") {
                triple.first.remove(triple.second)
            }
        }

        pendingSwitchStates.clear()
        pendingPickerStates.clear()
        pendingRowCheckedStates.clear()
        pendingRowVisibleStates.clear()
    }

    // =====================================================================================
    // Hỗ trợ đọc cấu hình dạng TOML (định dạng đầu vào duy nhất - đã loại bỏ hỗ trợ XML)
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
    // Thứ tự hiển thị: LUÔN theo đúng vị trí xuất hiện trong file (trên trước, dưới sau),
    // bất kể loại mục là gì (group, action, page, text ...) - kể cả khi các loại XEN KẼ
    // nhau (vd 1 action rồi tới 1 page rồi lại 1 action nữa). Vị trí này lấy trực tiếp từ
    // dòng khai báo `[[ten]]` trong file thông qua API vị trí của tomlj, không cần khai báo
    // thêm field `order` nào.
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
    //
    // Trang có RẤT NHIỀU mục / nhiều mục cần chạy shell (title-sh, desc-sh, switch...) nên
    // load lâu: đặt process = true trên [[page]] trỏ tới trang đó (cùng cấp với config /
    // config-sh) để trang hiện từng mục 1 ngay khi build xong (kèm thanh tiến trình dưới
    // toolbar) thay vì đợi build hết mới hiện toàn bộ. Ví dụ:
    //   [[page]]
    //   title = "Danh sách ứng dụng"
    //   config = "app_list.toml"
    //   process = true
    // =====================================================================================

    private val tomlNodeTypeOrder = listOf("group", "text", "switch", "picker", "action", "page", "editor", "resource", "menu", "fab")

    // ========== TÍNH NĂNG MỚI: [[menu]] / [[fab]] khai báo NGAY TRONG TOML CỦA CHÍNH TRANG ==========
    // Thay thế hoàn toàn cơ chế [[page.options]] cũ (từng gọi là "group.page.options") vốn khai
    // báo ở mục [[page]] của trang CHA - bị build eager ngay khi trang cha build xong, kể cả khi
    // trang con chưa từng được mở. Giờ đây menu 3 chấm (overflow) và nút nổi (fab) được khai báo
    // như 1 loại mục bình thường ngay trong file toml của CHÍNH trang đó - y hệt [[action]],
    // [[text]] ... - nên chỉ được đọc (và mọi lệnh shell bên trong, vd checked/box, chỉ chạy) khi
    // trang thực sự được mở, ĐÚNG LÚC các mục nội dung khác của trang cũng đang được đọc.
    //
    // [[menu]] / [[fab]] KHÔNG PHẢI là 1 mục đơn - mà là 1 "vỏ" chứa:
    //   - handler (hoặc handler-sh): script MẶC ĐỊNH chạy khi bấm 1 mục con, nếu mục con đó
    //     không tự khai báo "script" riêng. Thay thế cho handler-sh của page cũ (đã bỏ - xem
    //     PageNode.pageHandlerSh).
    //   - [[...items]]: danh sách các mục thực sự hiện trong menu/fab. Từng mục dùng field y hệt
    //     [[page.options]] cũ (type/style/suffix/mime/path-home/multiple/box/silent/link/
    //     activity/html/config/config-sh/script/title...) - xem pageMenuOptionToml().
    // Ví dụ:
    //   [[menu]]
    //   handler = "menu_handler.sh"
    //
    //     [[menu.items]]
    //     title = "Làm mới"
    //     type = "refresh"
    //
    //     [[menu.items]]
    //     title = "Bật X"
    //     type = "checkbox"
    //     box = "cat /sys/x/enabled"
    //     script = "toggle_x.sh"     # ghi đè handler dùng chung ở trên
    //
    //   [[fab]]
    //   handler = "fab_handler.sh"
    //
    //     [[fab.items]]
    //     title = "Thêm mới"
    //
    // Có thể đặt ở gốc file trang hoặc lồng trong 1 [[group]] (vd [[group.menu]], [[group.fab]])
    // giống hệt cách lồng action/page/text - dù đặt ở đâu, các mục menu/fab luôn được GOM VỀ 1
    // danh sách chung cho cả trang (không hiển thị trong danh sách nội dung, không lồng theo group).
    private val collectedMenuOptions = ArrayList<PageMenuOption>()

    /** Danh sách menu 3 chấm + fab gom được sau khi readConfigXml()/readConfigToml() chạy xong. */
    val pageMenuOptions: ArrayList<PageMenuOption> get() = collectedMenuOptions

    // ========== TÍNH NĂNG MỚI: [[group.action]] menu = true / show = true ==========
    // menu = true: action bị loại khỏi cây nội dung (không add vào group.children/danh sách
    // trang) và gom về đây thay - icon riêng LUÔN hiện trên toolbar, xem tomlBuildNode() case
    // "action" bên dưới và ActionPage.onCreateOptionsMenu().
    private val collectedHeaderActions = ArrayList<ActionNode>()
    /** Danh sách group.action có menu = true, gom được sau khi đọc xong toàn bộ trang. */
    val headerActions: ArrayList<ActionNode> get() = collectedHeaderActions

    // show = true: action tự mở dialog ngay khi vào trang - ĐỘC LẬP với menu (áp dụng được cho
    // cả action còn nằm trong danh sách lẫn action đã chuyển ra icon toolbar), nên gom riêng
    // sang đây, không gộp chung điều kiện với collectedHeaderActions ở trên.
    private val collectedAutoShowActions = ArrayList<ActionNode>()
    /** Danh sách group.action có show = true, gom được sau khi đọc xong toàn bộ trang. */
    val autoShowActions: ArrayList<ActionNode> get() = collectedAutoShowActions

    // Đọc 1 khối [[menu]]/[[fab]]: lấy handler dùng chung rồi build từng mục con trong "items",
    // mục nào không tự có "script" riêng thì dùng handler dùng chung này (gán thẳng vào
    // option.script lúc parse - lúc click chỉ cần đọc option.script, không cần fallback nào khác).
    private fun menuGroupOptionsToml(table: TomlTable, isFab: Boolean): ArrayList<PageMenuOption> {
        val handler = tomlGet(table, "handler", "handler-sh").orEmpty()
        val result = ArrayList<PageMenuOption>()
        for (itemTable in tomlEntries(table, "items")) {
            val option = pageMenuOptionToml(itemTable) ?: continue
            if (option.script.isEmpty() && handler.isNotEmpty()) {
                option.script = handler
            }
            option.isFab = isFab
            result.add(option)
        }
        return result
    }

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

    // Cho phép các thuộc tính kiểu boolean (vd readonly) nhận giá trị true/false/1/0
    // NHƯ CŨ, nhưng nếu giá trị không khớp các từ khoá đó thì coi là 1 đoạn lệnh shell,
    // chạy lệnh đó và coi kết quả trả về "1" là true, còn lại là false.
    // Ví dụ: readonly="echo 1"  ->  chạy `echo 1`, kết quả "1" -> readonly = true
    //        readonly="test -f /sdcard/lock && echo 1" -> readonly = true nếu file tồn tại
    private fun resolveBoolOrShell(raw: String?, vararg extraTruthyValues: String): Boolean {
        if (raw == null) return false
        val v = raw.trim()
        val lower = v.lowercase(getDefault())
        return when {
            lower.isEmpty() -> false
            lower == "1" || lower == "true" || extraTruthyValues.any { it == lower } -> true
            lower == "0" || lower == "false" -> false
            else -> executeResultRoot(context, v).trim() == "1"
        }
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

    private fun readConfigToml(rawText: String, onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase>? {
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
            // Chú ý: switch/picker được build ở đây chỉ ĐĂNG KÝ script lấy trạng thái (xem
            // pendingSwitchStates/pendingPickerStates) - trạng thái checked/value THẬT chỉ có
            // sau resolvePendingStates() bên dưới. Vì vậy khi process = true, các mục
            // switch/picker mới hiện ra qua onNodeReady có thể tạm hiện trạng thái mặc định;
            // bên gọi (ActionListFragment.finishProgressiveList) sẽ tự làm mới lại toàn bộ
            // hiển thị ngay sau khi resolvePendingStates() chạy xong ở dưới.
            val nodes = tomlChildren(result, onNodeReady)
            resolvePendingStates()
            nodes
        } catch (ex: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to parse configuration file\n" + ex.message, Toast.LENGTH_LONG).show()
            }
            Log.e("KrConfig Fail！", "" + ex.message)
            null
        }
    }

    // Lấy số dòng (trong file gốc) nơi 1 mục được khai báo, để dùng làm căn cứ sắp xếp
    // "trên trước, dưới sau". Với bảng mảng [[ten]] (nhiều mục cùng tên), phải lấy vị trí
    // của TỪNG phần tử qua TomlArray, vì vị trí của chính khoá `ten` chỉ trỏ tới mục ĐẦU
    // TIÊN. Nếu vì lý do gì đó không lấy được vị trí (API thay đổi, lỗi...), trả về null
    // và nơi gọi sẽ tự rơi về thứ tự xuất hiện khi đọc (seq) để không bao giờ crash.
    private fun tomlEntryLine(parent: TomlTable, key: String, index: Int): Int? {
        return try {
            if (parent.isArray(key)) {
                val arr: TomlArray? = parent.getArray(key)
                arr?.inputPositionOf(index)?.line()
            } else if (parent.isTable(key)) {
                parent.inputPositionOf(key)?.line()
            } else {
                null
            }
        } catch (ex: Exception) {
            null
        }
    }

    // Gom toàn bộ node con của 1 bảng (root, hoặc bảng của 1 group). Thứ tự hiển thị luôn
    // theo đúng vị trí xuất hiện trong file (trên trước, dưới sau), không phân biệt loại
    // mục và không cần field `order`.
    private fun tomlChildren(parent: TomlTable, onNodeReady: ((NodeInfoBase?, Int, Int) -> Unit)? = null): ArrayList<NodeInfoBase> {
        class Entry(val line: Int, val seq: Int, val type: String, val table: TomlTable) {
            var node: NodeInfoBase? = null
        }

        // Bước 1: chỉ THU THẬP vị trí (dòng) của từng mục - KHÔNG build, không chạy shell gì
        // cả (tomlEntries/tomlEntryLine chỉ đọc cấu trúc TOML), nên bước này rất rẻ dù trang
        // có bao nhiêu mục đi nữa. Nhờ vậy biết được tổng số mục (total) trước khi build.
        // `entries` ở đây đang theo thứ tự NHÓM LOẠI (group, text, switch, picker, action,
        // page, editor, resource - xem tomlNodeTypeOrder), y hệt thứ tự thu thập gốc.
        val entries = ArrayList<Entry>()
        var seq = 0
        for (type in tomlNodeTypeOrder) {
            for ((index, table) in tomlEntries(parent, type).withIndex()) {
                seq++
                val line = tomlEntryLine(parent, type, index) ?: Int.MAX_VALUE
                entries.add(Entry(line, seq, type, table))
            }
        }
        // Sắp theo dòng thực tế trong file; nếu không lấy được dòng (line == MAX_VALUE)
        // thì rơi về đúng thứ tự đọc được (seq) để vẫn ổn định, không xáo trộn ngẫu nhiên.
        val sortedEntries = entries.sortedWith(compareBy({ it.line }, { it.seq }))

        if (onNodeReady == null) {
            // Đường cũ, KHÔNG đổi hành vi: build tuần tự theo `entries` (thứ tự NHÓM LOẠI,
            // y hệt code gốc trước khi có tính năng process = true) - chỉ THỨ TỰ HIỂN THỊ
            // cuối cùng (giá trị trả về) mới sắp theo dòng, như code gốc vẫn luôn làm.
            for (entry in entries) {
                entry.node = tomlBuildNode(entry.type, entry.table)
            }
            return ArrayList(sortedEntries.mapNotNull { it.node })
        }

        // process = true: build TUẦN TỰ đúng theo vị trí dòng trong file (trên trước, dưới
        // sau - kể cả khi các loại mục xen kẽ nhau), báo ngay qua onNodeReady mỗi khi 1 mục
        // build xong, để ActionPage có thể hiện từng mục lên UI ngay lập tức thay vì đợi cả
        // trang build xong mới hiện - tránh "đơ"/timeout khi trang có rất nhiều mục cần shell.
        val total = sortedEntries.size
        var done = 0
        val result = ArrayList<NodeInfoBase>(total)
        for (entry in sortedEntries) {
            val node = tomlBuildNode(entry.type, entry.table)
            done++
            if (node != null) result.add(node)
            onNodeReady.invoke(node, done, total)
        }
        return result
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
            "action" -> {
                val action = actionNodeToml(table)
                if (action != null) {
                    if (action.show) collectedAutoShowActions.add(action)
                    if (action.menu) {
                        // Cần key duy nhất để làm itemId cho Menu (toolbar) - action thường
                        // không bắt buộc khai báo key/index/id, nên fallback về title nếu
                        // trống, giống hệt cách pageMenuOptionToml() đang làm cho [[menu.items]].
                        if (action.key.isEmpty()) action.key = action.title
                        collectedHeaderActions.add(action)
                        null
                    } else {
                        action
                    }
                } else {
                    null
                }
            }
            "switch" -> switchNodeToml(table)
            "picker" -> pickerNodeToml(table)
            "text" -> textNodeToml(table)
            "editor" -> editorNodeToml(table)
            "resource" -> {
                resourceNodeToml(table)
                null
            }
            "menu" -> {
                collectedMenuOptions.addAll(menuGroupOptionsToml(table, isFab = false))
                null
            }
            "fab" -> {
                collectedMenuOptions.addAll(menuGroupOptionsToml(table, isFab = true))
                null
            }
            else -> null
        }
    }

    private fun mainNodeToml(nodeInfoBase: NodeInfoBase, table: TomlTable): NodeInfoBase? {
        tomlGet(table, "support", "visible")?.let {
            if (!resolveBoolOrShell(it, "support", "visible")) return null
        }
        tomlGet(table, "key", "index", "id")?.let { nodeInfoBase.key = it.trim() }
        tomlGet(table, "title-sh")?.let {
            nodeInfoBase.titleSh = it
            nodeInfoBase.title = executeResultRoot(context, it)
        }
        if (nodeInfoBase.title.isEmpty()) {
            tomlGet(table, "title")?.let { nodeInfoBase.title = StringResRef.resolve(context, it) }
        }
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
            tomlGet(table, "icon-gif-num", "icon-gif_num")?.let { iconGifNum = it.trim().toIntOrNull() ?: iconGifNum }
            tomlGet(table, "icon-gif-time", "icon-gif_time")?.let { iconGifTime = it.trim().toIntOrNull() ?: iconGifTime }
            tomlGet(table, "icon-gif-autoplay", "icon-gif_autoplay")?.let { iconGifAutoplay = tomlTruthy(it) }
            tomlGet(table, "icon-gif-loop", "icon-gif-loop-count", "icon-gif_loop_count")?.let { iconGifLoopCount = it.trim().toIntOrNull() ?: iconGifLoopCount }
            tomlGet(table, "logo", "logo-path")?.let { logoPath = it.trim() }
            tomlGet(table, "photo", "photo-path")?.let { photoPath = it.trim() }
            tomlGet(table, "photo-real-size", "photo-original-size")?.let { photoRealSize = tomlTruthy(it, "real-size", "original-size") }
            tomlGet(table, "photo-gif-num", "gif-num", "gif_num")?.let { photoGifNum = it.trim().toIntOrNull() ?: photoGifNum }
            tomlGet(table, "photo-gif-time", "gif-time", "gif_time")?.let { photoGifTime = it.trim().toIntOrNull() ?: photoGifTime }
            tomlGet(table, "photo-gif-autoplay", "gif-autoplay", "gif_autoplay")?.let { photoGifAutoplay = tomlTruthy(it) }
            tomlGet(table, "photo-gif-loop", "photo-gif-loop-count", "gif-loop", "gif-loop-count", "gif_loop_count")?.let { photoGifLoopCount = it.trim().toIntOrNull() ?: photoGifLoopCount }
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
            tomlGet(table, "warn-sh", "warning-sh")?.let {
                warningSh = it
                warning = executeResultRoot(context, it)
            }
            if (warning.isEmpty()) {
                tomlGet(table, "warn", "warning")?.let { warning = it }
            }
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
        tomlGet(table, "title-sh")?.let {
            group.titleSh = it
            group.title = executeResultRoot(context, it)
        }
        if (group.title.isEmpty()) {
            tomlGet(table, "title")?.let { group.title = StringResRef.resolve(context, it) }
        }
        tomlGet(table, "support", "visible")?.let { group.supported = resolveBoolOrShell(it, "support", "visible") }
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
        tomlGet(table, "process")?.let { page.process = tomlTruthy(it, "process") }
        tomlGet(table, "link", "href")?.let { page.link = it }
        tomlGet(table, "activity", "a", "intent")?.let { page.activity = it }
        tomlGet(table, "lock", "lock-state")?.let { page.lockShell = it }

        // ĐÃ LOẠI BỎ: option-sh (script sinh menu 3 chấm động, khai báo ở [[page]] của trang
        // cha) VÀ handler-sh của page (script mặc định khi bấm menu/fab). Page giờ chỉ còn
        // nhiệm vụ MỞ TRANG cho nhanh (link/activity/config/config-sh) - không kiêm nhiệm gì
        // liên quan tới menu/fab nữa. Toàn bộ menu/fab (tĩnh lẫn có điều kiện qua support/box)
        // giờ khai báo NGAY TRONG [[menu]]/[[fab]] của CHÍNH trang đó, xem menuGroupOptionsToml()
        // bên dưới.

        // Giống text.rows / action.rows: cho phép page.rows hiển thị thêm các dòng rich-text bên dưới
        for (rowTable in tomlEntries(table, "rows")) {
            textRowToml(page.rows, rowTable)
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
        // Script chạy riêng cho option này, không cần dựa vào pageHandlerSh + $menu_id nữa
        tomlGet(table, "script", "set", "setstate")?.let { option.script = it }
        // type = "spinner": danh sách lựa chọn (tĩnh + động) và lệnh đọc giá trị đang chọn -
        // giống hệt cơ chế của pickerNodeToml() nhưng gắn vào menu item thay vì mục nội dung.
        tomlGet(table, "option-sh", "options-sh", "options-su")?.let {
            if (option.options == null) option.options = ArrayList()
            option.optionsSh = it
        }
        val spinnerOptionsToml = tomlEntries(table, "options")
        if (spinnerOptionsToml.isNotEmpty()) {
            if (option.options == null) option.options = ArrayList()
            for (optTable in spinnerOptionsToml) {
                option.options!!.add(selectItemToml(optTable))
            }
        }
        tomlGet(table, "get", "getstate")?.let { option.spinnerGetState = it }
        // title-sh đã được xử lý ở mainNodeToml() (qua runnableNodeToml ở trên); ở đây chỉ
        // đọc thêm alias "text" và chỉ áp dụng khi chưa có title-sh (tránh ghi đè kết quả shell)
        if (option.title.isEmpty()) {
            tomlGet(table, "title", "text")?.let { option.title = StringResRef.resolve(context, it) }
        }
        if (option.key.isEmpty()) option.key = option.title
        return option
    }

    private fun switchNodeToml(table: TomlTable): SwitchNode? {
        val switchNode = runnableNodeToml(SwitchNode(pageConfigAbsPath), table) as SwitchNode? ?: return null
        tomlGet(table, "get", "getstate")?.let { switchNode.getState = it }
        tomlGet(table, "set", "setstate")?.let { switchNode.setState = it }
        tomlGet(table, "lock", "lock-state")?.let { switchNode.lockShell = it }
        resourceNodeToml(table)

        // ========== FIX: KHÔNG chạy getState riêng lẻ ngay tại đây nữa ==========
        // Trước đây mỗi switch/checkbox tự gọi executeResultRoot() ngay lúc parse trang,
        // nghĩa là N checkbox trong menu = N round-trip shell TUẦN TỰ (nguyên nhân gây
        // delay 1-2s khi menu có từ 3 checkbox trở lên). Giờ chỉ ĐĂNG KÝ script vào hàng
        // chờ pendingSwitchStates; toàn bộ sẽ được gộp và chạy ĐÚNG 1 LẦN ở cuối
        // readConfigToml() qua resolvePendingStates().
        switchNode.checked = false // giá trị mặc định tạm thời, tới khi có kết quả gộp
        if (switchNode.getState.isNotEmpty()) {
            pendingSwitchStates.add(switchNode to switchNode.getState)
        }
        if (switchNode.setState == null) {
            switchNode.setState = ""
        }
        return switchNode
    }

    private fun selectItemToml(optTable: TomlTable): SelectItem {
        val item = SelectItem()
        tomlGet(optTable, "val", "value")?.let { item.value = it }
        tomlGet(optTable, "title-sh")?.let {
            item.titleSh = it
            item.title = executeResultRoot(context, it)
        }
        if (item.title.isNullOrEmpty()) {
            tomlGet(optTable, "title", "text")?.let { item.title = StringResRef.resolve(context, it) }
        }
        if (item.value == null) item.value = item.title
        return item
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
                picker.options!!.add(selectItemToml(optTable))
            }
        }
        resourceNodeToml(table)

        // ========== FIX: KHÔNG chạy getState riêng lẻ ngay tại đây nữa (giống switch) ==========
        if (picker.getState.isNullOrEmpty()) {
            picker.getState = ""
        } else {
            pendingPickerStates.add(picker to picker.getState!!)
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

        // Giống text.rows: cho phép action.rows hiển thị thêm các dòng rich-text bên dưới
        for (rowTable in tomlEntries(table, "rows")) {
            textRowToml(action.rows, rowTable)
        }

        tomlGet(table, "menu")?.let { action.menu = tomlTruthy(it, "menu") }
        tomlGet(table, "show")?.let { action.show = resolveBoolOrShell(it, "show") }

        resourceNodeToml(table)
        return action
    }

    private fun actionParamToml(table: TomlTable): ActionParamInfo? {
        val p = ActionParamInfo()
        tomlGet(table, "name")?.let { p.name = it }
        tomlGet(table, "label")?.let { p.label = it }
        tomlGet(table, "label-sh")?.let { p.labelSh = it }
        tomlGet(table, "placeholder")?.let { p.placeholder = it }
        tomlGet(table, "placeholder-sh")?.let { p.placeholderSh = it }
        tomlGet(table, "title")?.let { p.title = it }
        tomlGet(table, "title-sh")?.let { p.titleSh = it }
        tomlGet(table, "desc")?.let { p.desc = it }
        tomlGet(table, "desc-sh")?.let { p.descSh = it }
        // Ghi chú riêng khi checkbox/switch đang bật (xem ActionParamInfo.descOn)
        tomlGet(table, "desc-on", "on-desc", "desc-checked")?.let { p.descOn = it }
        tomlGet(table, "desc-on-sh", "on-desc-sh", "desc-checked-sh")?.let { p.descOnSh = it }
        tomlGet(table, "value")?.let { p.value = it }
        tomlGet(table, "type")?.let { p.type = it.lowercase(getDefault()).trim() }
        tomlGet(table, "suffix")?.let {
            val suffix = it.lowercase(getDefault()).trim()
            if (p.mime.isEmpty()) p.mime = Suffix2Mime().toMime(suffix)
            p.suffix = suffix
        }
        tomlGet(table, "mime")?.let { p.mime = it.lowercase(getDefault()) }
        tomlGet(table, "path-home", "home-path", "pathhome")?.let { p.pathHome = it.trim() }
        val readonlyRaw = tomlGet(table, "readonly")
        readonlyRaw?.let { raw ->
            // ========== FIX: readonly dạng shell không còn chạy NGAY lúc parse trang ==========
            // Trước đây gọi thẳng resolveBoolOrShell() -> executeResultRoot() ngay tại đây,
            // nghĩa là readonly="...lệnh shell..." bị thực thi ngay khi mở trang/menu (lúc
            // đọc config), dù người dùng chưa hề bấm vào action đó.
            // Giờ: nếu là giá trị tĩnh (true/false/1/0) thì giữ nguyên hành vi cũ, xử lý ngay.
            // Nếu là lệnh shell thì CHỈ LƯU LẠI vào readonlySh, không thực thi ở đây nữa.
            // readonlySh sau đó được gộp chạy cùng value-sh/options-sh (xem
            // ActionListFragment.actionExecute) đúng lúc người dùng mở dialog nhập tham số.
            val v = raw.trim()
            val lower = v.lowercase(getDefault())
            when {
                lower.isEmpty() -> p.readonly = false
                lower == "1" || lower == "true" || lower == "readonly" -> p.readonly = true
                lower == "0" || lower == "false" -> p.readonly = false
                else -> {
                    p.readonly = false // giá trị mặc định tạm thời, tới khi có kết quả shell
                    p.readonlySh = v
                }
            }
        }
        // ========== TÍNH NĂNG MỚI: sort (chỉ dùng được cùng readonly) ==========
        // ========== SỬA LỖI: điều kiện cũ sai - bắt buộc GIÁ TRỊ hiện tại phải là true ==========
        // Trước đây dùng "(p.readonly || !p.readonlySh.isNullOrEmpty())" - nghĩa là chỉ mục
        // ĐANG readonly=true mới được chấp nhận sort=true. Điều đó SAI vì các mục "sáng"
        // (readonly="false" hoặc căn bản không có readonly=true) - chính là các mục cần được
        // dồn LÊN TRÊN - sẽ không bao giờ đủ điều kiện tham gia nhóm sort, khiến nhóm chỉ toàn
        // mục xám, không có gì để đối chiếu, tính năng gần như vô dụng.
        // Đúng như depend-sort (chỉ cần đã khai báo depend-readonly=true - một MODE FLAG, không
        // quan tâm điều kiện hiện tại đúng/sai), ở đây điều kiện đúng phải là: param có khai báo
        // thuộc tính "readonly" trong config hay KHÔNG (bất kể giá trị true/false/shell) - tức
        // là param đó đang "tham gia hệ thống readonly" của dòng cấu hình, không phải đang thực
        // sự bị khóa hay không tại thời điểm parse.
        tomlGet(table, "sort")?.let { p.sort = (readonlyRaw != null) && tomlTruthy(it) }
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
            if (!resolveBoolOrShell(it, "support", "visible")) p.supported = false
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
        tomlGet(table, "depend-include-hidden")?.let { p.dependIncludeHidden = !(it == "false" || it == "0") }
        tomlGet(table, "depend-cascade")?.let { p.dependCascade = !(it == "false" || it == "0") }
        tomlGet(table, "depend-onchange", "depend-on-change", "depend-callback")?.let { p.dependOnChangeCallback = it }
        tomlGet(table, "depend-readonly")?.let { p.dependReadonly = tomlTruthy(it) }
        // ========== TÍNH NĂNG MỚI: depend-sort (chỉ dùng được cùng depend-readonly) ==========
        // Ép về false nếu depend-readonly không phải true, dù file cấu hình có khai báo
        // depend-sort="true" đi nữa - tránh hành vi khó hiểu (sort mục đang bị ẨN hẳn thì
        // không có ý nghĩa gì, vì View.GONE không chiếm chỗ để "dồn xuống dưới").
        tomlGet(table, "depend-sort")?.let { p.dependSort = p.dependReadonly && tomlTruthy(it) }
        tomlGet(table, "allow-no-selection", "no-select")?.let { p.allowNoSelection = tomlTruthy(it) }

        val paramOptions = tomlEntries(table, "options")
        if (paramOptions.isNotEmpty()) {
            if (p.options == null) p.options = ArrayList()
            for (optTable in paramOptions) {
                p.options!!.add(selectItemToml(optTable))
            }
        }

        return if (p.supported && !p.name.isNullOrEmpty()) p else null
    }

    private fun textNodeToml(table: TomlTable): TextNode? {
        val text = mainNodeToml(TextNode(pageConfigAbsPath), table) as TextNode? ?: return null
        for (rowTable in tomlEntries(table, "rows")) {
            textRowToml(text.rows, rowTable)
        }
        resourceNodeToml(table)
        return text
    }

    private fun textRowToml(rows: ArrayList<TextNode.TextRow>, table: TomlTable) {
        val row = TextNode.TextRow()

        // Ẩn/hiện row theo điều kiện (tĩnh true/false hoặc lệnh shell, giống support/visible của
        // param). Giá trị hằng số xử lý ngay như cũ; nếu là lệnh shell thì KHÔNG chạy ngay tại
        // đây nữa - đăng ký vào pendingRowVisibleStates để gộp 1 lần cùng switch/picker/row khác
        // trong resolvePendingStates() (tránh N round-trip tuần tự, xem comment ở pendingSwitchStates).
        // Row vẫn được thêm tạm vào "rows" để giữ đúng thứ tự; nếu shell trả về false sẽ bị xoá
        // khỏi danh sách sau khi resolve xong.
        var visibleShellScript: String? = null
        tomlGet(table, "support", "visible")?.let {
            val v = it.trim()
            val lower = v.lowercase(getDefault())
            when {
                lower.isEmpty() || lower == "1" || lower == "true" || lower == "support" || lower == "visible" -> {}
                lower == "0" || lower == "false" -> return
                else -> visibleShellScript = v
            }
        }

        tomlGet(table, "bold", "b")?.let { row.bold = tomlTruthy(it, "bold") }
        tomlGet(table, "italic", "i")?.let { row.italic = tomlTruthy(it, "italic") }
        tomlGet(table, "underline", "u")?.let { row.underline = tomlTruthy(it, "underline") }
        tomlGet(table, "strikethrough", "line-through", "delete-line", "del")?.let { row.strikethrough = tomlTruthy(it, "strikethrough", "line-through", "del") }
        tomlGet(table, "monospace", "mono", "code")?.let { row.monospace = tomlTruthy(it, "monospace", "mono", "code") }
        // letter-spacing: đơn vị em (giống TextView.letterSpacing), ví dụ 0.1 = giãn nhẹ, -0.05 = thu hẹp
        tomlGet(table, "letter-spacing", "letterspacing", "spacing")?.let { row.letterSpacing = it.trim().toFloatOrNull() ?: row.letterSpacing }
        // line-height: hệ số nhân chiều cao dòng, ví dụ 1.5 = cao hơn 50%, 0.8 = thấp hơn 20%
        tomlGet(table, "line-height", "lineheight", "row-height")?.let { row.lineHeight = it.trim().toFloatOrNull() ?: row.lineHeight }
        // margin-top/margin-bottom: khoảng trống (dp) thêm phía trên/dưới row này
        tomlGet(table, "margin-top", "spacing-top", "top-margin")?.let { row.marginTop = it.trim().toIntOrNull() ?: row.marginTop }
        tomlGet(table, "margin-bottom", "spacing-bottom", "bottom-margin")?.let { row.marginBottom = it.trim().toIntOrNull() ?: row.marginBottom }
        // alpha/opacity: nhận 0.0-1.0 (tỉ lệ) hoặc 0-255 (giá trị alpha kênh màu, tự quy đổi nếu > 1)
        tomlGet(table, "alpha", "opacity")?.let {
            val v = it.trim().toFloatOrNull()
            if (v != null) {
                row.alpha = (if (v > 1f) v / 255f else v).coerceIn(0f, 1f)
            }
        }
        tomlGet(table, "foreground", "color")?.let { try { row.color = it.toColorInt() } catch (_: Exception) {} }
        tomlGet(table, "bg", "background", "bgcolor")?.let { try { row.bgColor = it.toColorInt() } catch (_: Exception) {} }
        tomlGet(table, "size")?.let { row.size = it.trim().toIntOrNull() ?: row.size }
        tomlGet(table, "break")?.let { row.breakRow = tomlTruthy(it, "break") }
        tomlGet(table, "line", "divider", "separator")?.let { row.line = tomlTruthy(it, "line", "divider", "separator") }
        tomlGet(table, "link", "href")?.let { row.link = it }
        tomlGet(table, "activity", "a", "intent")?.let { row.activity = it }
        tomlGet(table, "photo", "photo-path")?.let { row.photo = it.trim() }
        tomlGet(table, "photo-real-size", "photo-original-size")?.let { row.photoRealSize = tomlTruthy(it, "real-size", "original-size") }
        tomlGet(table, "photo-gif-num", "gif-num", "gif_num")?.let { row.photoGifNum = it.trim().toIntOrNull() ?: row.photoGifNum }
        tomlGet(table, "photo-gif-time", "gif-time", "gif_time")?.let { row.photoGifTime = it.trim().toIntOrNull() ?: row.photoGifTime }
        tomlGet(table, "photo-gif-autoplay", "gif-autoplay", "gif_autoplay")?.let { row.photoGifAutoplay = tomlTruthy(it) }
        tomlGet(table, "photo-gif-loop", "photo-gif-loop-count", "gif-loop", "gif-loop-count", "gif_loop_count")?.let { row.photoGifLoopCount = it.trim().toIntOrNull() ?: row.photoGifLoopCount }
        // icon: ảnh nhỏ hiển thị NGAY CẠNH chữ (inline), khác "photo" (khối ảnh riêng full chiều rộng)
        tomlGet(table, "icon", "icon-path")?.let { row.icon = it.trim() }
        tomlGet(table, "icon-position", "icon-pos")?.let {
            val p = it.trim().lowercase(getDefault())
            row.iconPosition = when (p) {
                "after", "end", "right" -> "after"
                else -> "before"
            }
        }
        tomlGet(table, "icon-size")?.let { row.iconSize = it.trim().toIntOrNull() ?: row.iconSize }
        tomlGet(table, "script", "run")?.let { row.onClickScript = it }
        tomlGet(table, "sh")?.let { row.dynamicTextSh = it }
        // Toggle nhỏ (checkbox / switch) lồng trong dòng text
        tomlGet(table, "toggle", "toggle-type")?.let {
            val t = it.trim().lowercase(getDefault())
            if (t == "checkbox" || t == "switch") row.toggle = t
        }
        // checked: hằng số xử lý ngay; lệnh shell thì đăng ký vào pendingRowCheckedStates - gộp
        // chung với các row/switch/picker khác trong resolvePendingStates() thay vì chạy riêng lẻ.
        tomlGet(table, "checked", "checked-sh", "check")?.let {
            val v = it.trim()
            val lower = v.lowercase(getDefault())
            row.checked = when {
                lower.isEmpty() -> false
                lower == "1" || lower == "true" || lower == "checked" || lower == "check" -> true
                lower == "0" || lower == "false" -> false
                else -> {
                    pendingRowCheckedStates.add(row to v)
                    false // giá trị mặc định tạm thời, tới khi có kết quả gộp
                }
            }
        }
        tomlGet(table, "onchange-sh", "on-change-sh", "toggle-sh", "set-sh")?.let { row.onChangeSh = it }
        tomlGet(table, "align")?.let {
            when (it) {
                "opposite" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) row.align = Layout.Alignment.ALIGN_OPPOSITE
                "center" -> row.align = Layout.Alignment.ALIGN_CENTER
                "normal" -> row.align = Layout.Alignment.ALIGN_NORMAL
            }
        }
        tomlGet(table, "text")?.let { row.text = StringResRef.resolve(context, it) }
        rows.add(row)
        visibleShellScript?.let { pendingRowVisibleStates.add(Triple(rows, row, it)) }
    }

    private fun editorNodeToml(table: TomlTable): EditorNode? {
        val editor = clickableNodeToml(EditorNode(pageConfigAbsPath), table) as EditorNode? ?: return null
        tomlGet(table, "file", "path")?.let { editor.file = it.trim() }
        tomlGet(table, "wrap")?.let { editor.wrap = !(it == "0" || it == "false" || it == "off" || it == "no-wrap") }
        tomlGet(table, "placeholder")?.let { editor.placeholder = it }
        tomlGet(table, "readonly")?.let { editor.readonly = resolveBoolOrShell(it) }
        tomlGet(table, "need-input")?.let { editor.needInput = (it == "true" || it == "1") }
        tomlGet(table, "value-sh")?.let { editor.valueSh = it }
        tomlGet(table, "value")?.let { editor.value = it }
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