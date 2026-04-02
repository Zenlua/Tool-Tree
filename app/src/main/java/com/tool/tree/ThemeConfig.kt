package com.tool.tree

import android.content.Context
import androidx.core.content.edit
import com.tool.tree.MainActivity

class ThemeConfig(private val context: Context) {
    private val config = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE)

    fun getAllowTransparentUI(): Boolean {
        return config.getBoolean("TransparentUI", false)
    }

    fun setAllowTransparentUI(allow: Boolean) {
        config.edit { putBoolean("TransparentUI", allow) }
        (context as? MainActivity)?.reloadTabs()
    }

    fun getAllowNotificationUI(): Boolean {
        return config.getBoolean("NotificationUI", true)
    }

    fun setAllowNotificationUI(allow: Boolean) {
        config.edit { putBoolean("NotificationUI", allow) }
        (context as? MainActivity)?.reloadTabs()
        val appContext = context.applicationContext
        if (allow) {
            WakeLockService.startService(appContext)
        } else {
            WakeLockService.stopService(appContext)
        }
    }
}