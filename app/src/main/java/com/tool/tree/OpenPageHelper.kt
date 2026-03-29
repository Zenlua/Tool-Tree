package com.tool.tree

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.krscript.model.PageNode
import com.omarea.common.ui.ProgressBarDialog

class OpenPageHelper(private val activity: Activity) {
    private val progressBarDialog by lazy { ProgressBarDialog(activity) }

    fun openPage(pageNode: PageNode) {
        try {
            val intent: Intent? = when {
                pageNode.onlineHtmlPage.isNotEmpty() -> {
                    progressBarDialog.showDialog(activity.getString(R.string.please_wait))
                    // Tạo Intent
                    Intent(activity, ActionPageOnline::class.java).apply {
                        putExtra("config", pageNode.onlineHtmlPage)
                    }
                }

                pageNode.pageConfigSh.isNotEmpty() || pageNode.pageConfigPath.isNotEmpty() -> {
                    Intent(activity, ActionPage::class.java)
                }

                else -> null
            }

            intent?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("page", pageNode)
                activity.startActivity(this)
            }

            // Ẩn dialog sau khi startActivity
            if (pageNode.onlineHtmlPage.isNotEmpty()) {
                Handler(Looper.getMainLooper()).post {
                    progressBarDialog.hideDialog()
                }
            }

        } catch (ex: Exception) {
            Toast.makeText(activity, ex.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }
}