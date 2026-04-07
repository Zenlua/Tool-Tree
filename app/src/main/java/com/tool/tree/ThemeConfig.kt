package com.tool.tree

import android.app.Activity
import android.content.Context
import androidx.core.content.edit

class ThemeConfig(private val context: Context) {

    private val config = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE)

    // Lưu theme mode 0–5
    fun getThemeMode(): Int {
        return config.getInt("ThemeMode", 0) // mặc định 0
    }

    fun setThemeMode(mode: Int) {
        val newMode = mode.coerceIn(0, 5)
        config.edit { putInt("ThemeMode", newMode) }
        (context as? Activity)?.recreate()
    }

    // Nếu muốn, vẫn giữ các boolean riêng cho notification UI
    fun getAllowNotificationUI(): Boolean {
        return config.getBoolean("NotificationUI", true)
    }

    fun setAllowNotificationUI(allow: Boolean) {
        config.edit { putBoolean("NotificationUI", allow) }
        val appContext = context.applicationContext
        if (allow) {
            WakeLockService.startService(appContext)
        } else {
            WakeLockService.stopService(appContext)
        }
    }
}