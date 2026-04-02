package com.tool.tree

import android.app.Activity
import android.content.Context
import androidx.core.content.edit

class ThemeConfig(private val context: Context) {
    private val config = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE)

    fun getAllowTransparentUI(): Boolean {
        return config.getBoolean("TransparentUI", false)
    }

    fun setAllowTransparentUI(allow: Boolean) {
        config.edit { putBoolean("TransparentUI", allow) }
        if (context is Activity) {
            context.recreate()
        }
    }

    fun getAllowNotificationUI(): Boolean {
        return config.getBoolean("NotificationUI", true)
    }

    fun setAllowNotificationUI(allow: Boolean) {
        config.edit { putBoolean("NotificationUI", allow) }
        if (context is Activity) {
            context.recreate()
        }
        val appContext = context.applicationContext
        if (allow) {
            WakeLockService.startService(appContext)
        } else {
            WakeLockService.stopService(appContext)
        }
    }
}