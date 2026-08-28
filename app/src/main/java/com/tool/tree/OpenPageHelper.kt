package com.tool.tree

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.omarea.common.ui.DialogHelper
import com.omarea.common.ui.ProgressBarDialog
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import com.omarea.krscript.executor.ScriptEnvironmen
import com.omarea.krscript.model.ActionNode
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.PageMenuOption
import com.omarea.krscript.model.PageNode
import com.tool.tree.ui.SwipeBackPreviewCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class OpenPageHelper(private val activity: Activity) {

    companion object {
        // Thời gian tối đa chờ tải trước nội dung html (ms) - quá thời gian này vẫn cứ mở
        // trang bình thường (không kèm nội dung preload), để trang tự tải lại từ đầu như hành
        // vi cũ thay vì bắt người dùng chờ vô hạn khi mạng chậm/treo.
        private const val HTML_PRELOAD_TIMEOUT_MS = 10000L
        // Giới hạn dung lượng tải trước - phòng trường hợp "onlineHtmlPage" trỏ nhầm sang 1
        // file rất lớn (không phải trang html thông thường), tránh tốn bộ nhớ vô ích.
        private const val HTML_PRELOAD_MAX_BYTES = 8L * 1024 * 1024
    }

    /**
     * @param onNoNavigate gọi khi hàm này KHÔNG mở Activity mới (mục không hợp lệ, bị khoá,
     * tải lỗi, hoặc người dùng bấm Hủy lúc đang preload) - để bên gọi tự dọn lại state của
     * riêng nó (ví dụ ActionPage._openPage() reset cờ openedSubPage) vì sẽ KHÔNG có
     * onRestart()/onResume() nào bắn ra do không hề có Activity con nào được mở.
     */
    fun openPage(pageNode: PageNode, onNoNavigate: (() -> Unit)? = null) {
        try {
            val needsConfigLoad = pageNode.pageConfigSh.isNotEmpty() || pageNode.pageConfigPath.isNotEmpty()

            // process = false: tải toàn bộ items + menu/fab NGAY TẠI trang cha (hiện dialog
            // loading "ở ngoài"), chỉ mở trang mới sau khi đã có sẵn nội dung để hiện ra tức
            // thì - tránh cảnh mở trang mới ra rồi mới thấy dialog loading bên trong nó.
            // process = true (progressive) giữ nguyên hành vi cũ: mở trang ngay, tự tải + hiện
            // dần bên trong, vì đó chính là mục đích của progressive load.
            if (needsConfigLoad && !pageNode.process) {
                preloadThenOpen(pageNode, onNoNavigate)
                return
            }

            // Giống hệt cơ chế preload của config page ở trên: hiện dialog loading NGAY TẠI
            // trang cha, tải trước nội dung html qua mạng, CHỈ mở ActionPageOnline sau khi đã
            // có sẵn nội dung - tránh cảnh mở Activity ra rồi mới thấy loading bên trong
            // WebView như trước. Cũng tôn trọng "process = true" để bỏ qua bước này (mở ngay,
            // tự tải bên trong) y hệt quy ước của config page.
            if (pageNode.onlineHtmlPage.isNotEmpty() && !pageNode.process) {
                preloadHtmlThenOpen(pageNode, onNoNavigate)
                return
            }

            if (!openPageDirect(pageNode, null)) {
                onNoNavigate?.invoke()
            }
        } catch (ex: Exception) {
            onNoNavigate?.invoke()
            Toast.makeText(activity, ex.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mở Activity trang đích - TÁCH riêng khỏi openPage() để preloadThenOpen() gọi lại đúng 1
     * lần duy nhất sau khi đã tải xong (kèm theo [preloaded] nếu có), không lặp lại logic tạo
     * Intent/SwipeBackPreviewCache ở 2 nơi.
     * @return false nếu pageNode không khớp bất kỳ loại trang nào (không có Activity nào được mở).
     */
    private fun openPageDirect(
        pageNode: PageNode,
        preloaded: PagePreloadedData?,
        preloadedHtml: String? = null,
        preloadedHtmlBaseUrl: String? = null
    ): Boolean {
        val intent = when {
            pageNode.onlineHtmlPage.isNotEmpty() -> {
                Intent(activity, ActionPageOnline::class.java).apply {
                    putExtra("config", pageNode.onlineHtmlPage)
                    if (!preloadedHtml.isNullOrEmpty()) {
                        putExtra("preloadedHtml", preloadedHtml)
                        putExtra("preloadedHtmlBaseUrl", preloadedHtmlBaseUrl ?: pageNode.onlineHtmlPage)
                    }
                }
            }
    
            pageNode.pageConfigSh.isNotEmpty() ||
            pageNode.pageConfigPath.isNotEmpty() -> {
                Intent(activity, ActionPage::class.java)
            }
    
            else -> null
        } ?: return false
    
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("page", pageNode)
            if (preloaded != null) {
                putExtra("preloadedItems", preloaded)
            }
            activity.window.decorView.post {
                SwipeBackPreviewCache.capture(activity) {
                    activity.startActivity(this)
                }
            }
        }
        return true
    }



    /**
     * process = false: chạy lại ĐÚNG pipeline mà ActionPage.checkPageLockThenLoad() +
     * loadPageConfig(process=false) vẫn làm (kiểm tra khoá -> beforeRead -> đọc/build toàn bộ
     * items+menu/fab -> afterRead -> loadSuccess/loadFail), nhưng chạy NGAY TẠI activity hiện
     * tại (trang cha) với dialog loading hiện ở đây, thay vì mở trang mới rồi mới tải bên
     * trong nó. Chỉ khi build xong THÀNH CÔNG mới thật sự mở trang mới (kèm sẵn dữ liệu qua
     * PagePreloadedData) - nếu bị khoá hoặc lỗi thì báo NGAY tại đây, không mở trang.
     */
    private fun preloadThenOpen(pageNode: PageNode, onNoNavigate: (() -> Unit)?) {
        val owner = activity as? LifecycleOwner
        if (owner == null) {
            // Không có LifecycleScope để chạy nền + tự hủy theo vòng đời (activity không phải
            // AppCompatActivity) - fallback về hành vi CŨ (mở trang rồi tự tải bên trong) thay
            // vì treo mãi ở đây.
            if (!openPageDirect(pageNode, null)) {
                onNoNavigate?.invoke()
            }
            return
        }

        val dialog = ProgressBarDialog(activity)
        var job: Job? = null
        dialog.setCancelCallback {
            job?.cancel()
            onNoNavigate?.invoke()
        }
        dialog.showDialog(activity.getString(R.string.kr_page_loading))

        job = owner.lifecycleScope.launch(Dispatchers.IO) {
            // 1. Kiểm tra khoá của TRANG SẮP MỞ - gộp vào đây cho mục process=false thay vì để
            // ActionPage tự kiểm tra sau khi đã mở (xem ActionPage.checkPageLockThenLoad cũ) -
            // nhờ vậy bị khoá thì KHÔNG mở trang mới nữa, báo thẳng tại đây.
            if (pageNode.lockShell.isNotEmpty()) {
                val message = ScriptEnvironmen.executeResultRoot(activity, pageNode.lockShell, pageNode)
                val unlocked = message == "unlock" || message == "unlocked" || message == "false" || message == "0"
                if (!unlocked) {
                    withContext(Dispatchers.Main) {
                        dialog.hideDialog()
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            DialogHelper.helpInfo(
                                activity,
                                activity.getString(R.string.kr_lock_title),
                                message.ifEmpty { activity.getString(R.string.kr_lock_message) }
                            )
                        }
                        onNoNavigate?.invoke()
                    }
                    return@launch
                }
            } else if (pageNode.locked) {
                withContext(Dispatchers.Main) {
                    dialog.hideDialog()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        DialogHelper.helpInfo(
                            activity,
                            activity.getString(R.string.kr_lock_title),
                            activity.getString(R.string.kr_lock_message)
                        )
                    }
                    onNoNavigate?.invoke()
                }
                return@launch
            }

            // 2. beforeRead
            if (pageNode.beforeRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(activity, pageNode.beforeRead, pageNode)
            }

            // 3. Đọc + build TOÀN BỘ items/menu/fab (process=false nên không cần onNodeReady -
            // không có UI progressive nào đang đợi cập nhật dần cả)
            var items: ArrayList<NodeInfoBase>? = null
            var menuOptions: ArrayList<PageMenuOption>? = null
            var headerActions: ArrayList<ActionNode>? = null
            var autoShowActions: ArrayList<ActionNode>? = null

            if (pageNode.pageConfigSh.isNotEmpty()) {
                val shReader = PageConfigSh(activity, pageNode.pageConfigSh, pageNode)
                items = shReader.execute()
                menuOptions = shReader.pageMenuOptions
                headerActions = shReader.headerActions
                autoShowActions = shReader.autoShowActions
            }
            if (items == null && pageNode.pageConfigPath.isNotEmpty()) {
                val reader = PageConfigReader(activity.applicationContext, pageNode.pageConfigPath, pageNode.pageConfigDir)
                items = reader.readConfigXml()
                menuOptions = reader.pageMenuOptions
                headerActions = reader.headerActions
                autoShowActions = reader.autoShowActions
            }

            if (pageNode.afterRead.isNotEmpty()) {
                ScriptEnvironmen.executeResultRoot(activity, pageNode.afterRead, pageNode)
            }

            withContext(Dispatchers.Main) {
                if (!isActive) return@withContext
                dialog.hideDialog()

                // Trang có thể không có items nhưng vẫn hợp lệ nếu có menu/fab - giống điều
                // kiện ActionPage.loadPageConfig() đang dùng.
                val hasMenuOrFab = menuOptions?.isNotEmpty() == true || headerActions?.isNotEmpty() == true
                if (items != null && (items.isNotEmpty() || hasMenuOrFab)) {
                    if (pageNode.loadSuccess.isNotEmpty()) {
                        ScriptEnvironmen.executeResultRoot(activity, pageNode.loadSuccess, pageNode)
                    }
                    val preloaded = PagePreloadedData(items, menuOptions, headerActions, autoShowActions)
                    if (!openPageDirect(pageNode, preloaded)) {
                        onNoNavigate?.invoke()
                    }
                } else {
                    if (pageNode.loadFail.isNotEmpty()) {
                        ScriptEnvironmen.executeResultRoot(activity, pageNode.loadFail, pageNode)
                    }
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        Toast.makeText(activity, activity.getString(R.string.kr_page_load_fail), Toast.LENGTH_SHORT).show()
                    }
                    onNoNavigate?.invoke()
                }
            }
        }
    }

    /**
     * Tương tự preloadThenOpen() nhưng dành cho trang html online (onlineHtmlPage): tải trước
     * NỘI DUNG html qua mạng NGAY TẠI trang cha (kèm dialog loading ở đây), CHỈ mở
     * ActionPageOnline sau khi đã tải xong - ActionPageOnline sẽ hiện thẳng nội dung đã có sẵn
     * qua loadDataWithBaseURL() thay vì tự loadUrl() lại từ đầu (xem
     * ActionPageOnline.initWebview()).
     *
     * CHỦ ĐỘNG không tải trước (mở thẳng như cũ) khi: không có LifecycleScope để chạy nền, hoặc
     * URL không phải http/https (ví dụ trang html đóng gói sẵn trong assets - vốn đã hiện tức
     * thì, tải trước ở đây chỉ tổ chậm thêm không cần thiết).
     *
     * Nếu tải lỗi/hết thời gian/không phải nội dung html - KHÔNG báo lỗi ở đây, chỉ đơn giản mở
     * trang bình thường (không kèm preload) để chính WebView bên trong ActionPageOnline tự tải
     * lại và tự hiện lỗi (nếu có) như hành vi cũ - tránh phải xử lý lại UI báo lỗi ở 2 nơi.
     */
    private fun preloadHtmlThenOpen(pageNode: PageNode, onNoNavigate: (() -> Unit)?) {
        val url = pageNode.onlineHtmlPage
        val owner = activity as? LifecycleOwner
        if (owner == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            if (!openPageDirect(pageNode, null)) {
                onNoNavigate?.invoke()
            }
            return
        }

        val dialog = ProgressBarDialog(activity)
        var job: Job? = null
        dialog.setCancelCallback {
            job?.cancel()
            onNoNavigate?.invoke()
        }
        dialog.showDialog(activity.getString(R.string.kr_page_loading))

        // "Làm nóng" WebView TRƯỚC khi bắt đầu tải mạng - lần đầu tạo WebView trong tiến trình,
        // Android phải khởi động engine Chromium (khá nặng, có thể mất vài trăm ms). Nếu không
        // làm trước ở đây, chi phí này sẽ rơi đúng vào lúc ActionPageOnline.onCreate() tạo
        // WebView thật - tức là ngay SAU KHI dialog loading vừa đóng - đúng lúc người dùng cảm
        // nhận độ trễ rõ nhất ("đóng dialog xong mới vào trang"). Gọi ở đây thay vào đó để chi
        // phí này bị "giấu" trong lúc dialog loading vẫn còn đang hiện (dialog đã show() xong ở
        // trên trước khi gọi hàm này) - dialog có hiện lâu hơn 1 chút cũng không sao vì người
        // dùng đang trông đợi có loading sẵn, khác hẳn cảm giác khựng lại SAU KHI loading đã
        // biến mất. Tạo trước rồi hủy ngay - KHÔNG giữ lại dùng (ActionPageOnline vẫn tự tạo
        // WebView riêng của nó), chỉ nhằm buộc phần khởi tạo native 1 LẦN xảy ra sớm.
        warmUpWebViewEngine()

        job = owner.lifecycleScope.launch(Dispatchers.IO) {
            val fetched = try {
                withTimeoutOrNull(HTML_PRELOAD_TIMEOUT_MS) { fetchHtml(url) }
            } catch (_: Exception) {
                null
            }

            withContext(Dispatchers.Main) {
                if (!isActive) return@withContext
                dialog.hideDialog()
                if (!openPageDirect(pageNode, null, fetched?.first, fetched?.second)) {
                    onNoNavigate?.invoke()
                }
            }
        }
    }

    /**
     * Tạo thử 1 WebView (dùng applicationContext, không gắn vào bất kỳ layout nào) rồi hủy ngay
     * - chỉ nhằm kích hoạt sớm bước khởi tạo engine Chromium (nếu đây là lần đầu tiên trong
     * tiến trình app tạo WebView). PHẢI gọi trên main thread (mọi thao tác WebView đều vậy) -
     * hàm này được gọi đồng bộ ngay tại preloadHtmlThenOpen(), TRƯỚC khi launch coroutine tải
     * mạng, nên có chặn main thread trong lúc khởi tạo (chỉ 1 lần đầu tiên - các lần sau engine
     * đã sẵn, gọi lại gần như tức thì) - chấp nhận được vì dialog loading đã hiện sẵn lúc này.
     */
    private fun warmUpWebViewEngine() {
        try {
            android.webkit.WebView(activity.applicationContext).destroy()
        } catch (_: Exception) {
            // Một số ROM/thiết bị thiếu WebView provider hợp lệ - bỏ qua, ActionPageOnline sẽ
            // tự báo lỗi khi thật sự cần tạo WebView (như hành vi vốn có từ trước).
        }
    }

    /**
     * Tải toàn bộ nội dung html (text) của [url] qua HTTP(S), theo redirect tới cùng.
     * @return Pair(nội dung html, url cuối cùng sau redirect) nếu thành công, null nếu lỗi/hết
     * dung lượng cho phép/không phải nội dung dạng text (html/xml) - bên gọi tự fallback về mở
     * trang bình thường khi trả về null, KHÔNG ném exception ra ngoài.
     */
    private fun fetchHtml(url: String): Pair<String, String>? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = HTML_PRELOAD_TIMEOUT_MS.toInt()
                readTimeout = HTML_PRELOAD_TIMEOUT_MS.toInt()
                instanceFollowRedirects = true
                setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                return null
            }

            val contentType = connection.contentType ?: ""
            // Bỏ qua nếu rõ ràng không phải nội dung dạng text (ảnh/file nhị phân...) - để
            // WebView tự tải lại theo cách cũ thay vì hiện linh tinh từ dữ liệu preload sai kiểu.
            if (contentType.isNotEmpty() &&
                !contentType.contains("text", ignoreCase = true) &&
                !contentType.contains("html", ignoreCase = true) &&
                !contentType.contains("xml", ignoreCase = true)
            ) {
                return null
            }

            val charsetName = Regex("charset=([\\w-]+)", RegexOption.IGNORE_CASE)
                .find(contentType)?.groupValues?.get(1) ?: "UTF-8"

            // Với java.net.HttpURLConnection (instanceFollowRedirects=true), sau connect() thành
            // công thì getURL() đã phản ánh URL cuối cùng (sau khi theo hết chuỗi redirect) -
            // dùng làm baseUrl để các link/script/ảnh tương đối trong trang tiếp tục phân giải
            // đúng khi hiện lại bằng loadDataWithBaseURL().
            val finalUrl = connection.url?.toString() ?: url

            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(chunk)
                    if (read == -1) break
                    total += read
                    if (total > HTML_PRELOAD_MAX_BYTES) return null
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            }

            val html = try {
                String(bytes, charset(charsetName))
            } catch (_: Exception) {
                String(bytes, Charsets.UTF_8)
            }
            html to finalUrl
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}