package com.tool.tree

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.omarea.krscript.model.PageNode
import com.tool.tree.ui.SwipeBackPreviewCache

class OpenPageHelper(private val activity: Activity) {

    /**
     * Luôn mở Activity trang đích NGAY LẬP TỨC, không preload/tải trước ở trang cha nữa:
     * - Page (config/config-sh): ActionPage tự lo checkPageLockThenLoad() -> loadPageConfig(),
     *   và TỰ hiện dialog loading (progressBarDialog) NGAY BÊN TRONG trang mới khi
     *   process = false (xem ActionPage.loadPageConfig(), useProgressiveLoad = showLoading &&
     *   config.process) - đây chính là hành vi mong muốn: dialog loading hiện ở trang mới, thay
     *   vì hiện "ở ngoài" trang cha trước khi điều hướng.
     * - Html (onlineHtmlPage): LUÔN mở ActionPageOnline ngay và để WebView tự loadUrl() +
     *   tự hiện loading bên trong nó (webViewInjector.showLoading/hideLoading) - coi như luôn
     *   ứng xử như process = true, BẤT KỂ pageNode.process đang là gì.
     *
     * @param onNoNavigate gọi khi hàm này KHÔNG mở được Activity mới (pageNode không khớp bất kỳ
     * loại trang nào) - để bên gọi tự dọn lại state của riêng nó (ví dụ ActionPage._openPage()
     * reset cờ openedSubPage) vì sẽ KHÔNG có onRestart()/onResume() nào bắn ra do không hề có
     * Activity con nào được mở.
     */
    fun openPage(pageNode: PageNode, onNoNavigate: (() -> Unit)? = null) {
        try {
            if (!openPageDirect(pageNode)) {
                onNoNavigate?.invoke()
            }
        } catch (ex: Exception) {
            onNoNavigate?.invoke()
            Toast.makeText(activity, ex.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mở Activity trang đích.
     * @return false nếu pageNode không khớp bất kỳ loại trang nào (không có Activity nào được mở).
     */
    private fun openPageDirect(pageNode: PageNode): Boolean {
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
            SwipeBackPreviewCache.capture(activity) {
                activity.startActivity(this)
            }
        }
        return true
    }
}
