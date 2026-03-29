package com.tool.tree

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.omarea.krscript.model.PageNode

class OpenPageHelper(private val activity: Activity) {

    fun openPage(pageNode: PageNode) {
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
