package com.tool.tree

import android.app.Activity
import android.content.Context
import androidx.core.content.edit

class ThemeConfig(private val context: Context) {

    private val config = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME_MODE = "ThemeMode"
        const val KEY_NOTIFICATION_UI = "NotificationUI"
    }

    fun getThemeMode(): Int {
        return config.getInt(KEY_THEME_MODE, 0)
    }

    fun setThemeMode(mode: Int) {
        val newMode = mode.coerceIn(0, 5)
        if (getThemeMode() != newMode) {
            config.edit { putInt(KEY_THEME_MODE, newMode) }
            if (context is Activity) {
                context.recreate()
            }
        }
    }

    fun getAllowNotificationUI(): Boolean {
        return config.getBoolean(KEY_NOTIFICATION_UI, true)
    }

    fun setAllowNotificationUI(allow: Boolean) {
        val current = getAllowNotificationUI()
        if (current == allow) return
        config.edit { putBoolean(KEY_NOTIFICATION_UI, allow) }
        val appContext = context.applicationContext
        if (allow) {
            WakeLockService.startService(appContext)
        } else {
            WakeLockService.stopService(appContext)
        }
    }
}
