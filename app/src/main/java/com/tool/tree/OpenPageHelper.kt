package com.tool.tree

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.krscript.model.PageNode

class OpenPageHelper(private val activity: Activity) {

    // companion object giúp biến này được chia sẻ chung, không bị reset khi khởi tạo lại OpenPageHelper
    companion object {
        private var lastOpenTime: Long = 0
        private const val MIN_CLICK_INTERVAL = 1000L // 1 giây chặn click nhanh
    }

    fun openPage(pageNode: PageNode) {
        val currentTime = System.currentTimeMillis()
        // Nếu khoảng cách giữa 2 lần bấm nhỏ hơn 1 giây, bỏ qua không xử lý
        if (currentTime - lastOpenTime < MIN_CLICK_INTERVAL) {
            return
        }
        lastOpenTime = currentTime

        try {
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
            }

            intent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("page", pageNode)
                activity.startActivity(this)
            }

        } catch (ex: Exception) {
            Toast.makeText(activity, ex.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }
}
