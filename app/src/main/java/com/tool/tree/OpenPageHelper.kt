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

class OpenPageHelper(private val activity: Activity) {

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
    private fun openPageDirect(pageNode: PageNode, preloaded: PagePreloadedData?): Boolean {
        val intent = when {
            pageNode.onlineHtmlPage.isNotEmpty() -> {
                Intent(activity, ActionPageOnline::class.java).apply {
                    putExtra("config", pageNode.onlineHtmlPage)
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
            // Chụp lại màn hình hiện tại NGAY TRƯỚC khi mở trang mới, để trang mới có thể
            // hiện lại nó làm nền phía sau lúc vuốt để trở lại (xem SwipeBackHelper).
            // capture() dùng PixelCopy nên là bất đồng bộ (để giữ đúng bo góc từng item -
            // xem SwipeBackPreviewCache) - PHẢI startActivity() bên trong callback, không
            // gọi ngay sau capture() như trước nữa.
            SwipeBackPreviewCache.capture(activity) {
                activity.startActivity(this)
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
}
