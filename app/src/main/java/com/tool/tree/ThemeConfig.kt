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

    // Lưu theme mode 0–5
    fun getThemeMode(): Int {
        return config.getInt(KEY_THEME_MODE, 0)
    }

    fun setThemeMode(mode: Int) {
        val newMode = mode.coerceIn(0, 5)
        // Chỉ xử lý nếu mode thực sự thay đổi
        if (getThemeMode() != newMode) {
            config.edit { putInt(KEY_THEME_MODE, newMode) }
            
            // Recreate chỉ hoạt động nếu context là Activity
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
        if (current == allow) return // Không có thay đổi thì không làm gì cả

        config.edit { putBoolean(KEY_NOTIFICATION_UI, allow) }
        
        val appContext = context.applicationContext
        if (allow) {
            // Khi bật, khởi động Service để hiện thông báo
            WakeLockService.startService(appContext)
        } else {
            // Khi tắt, dừng hoàn toàn Service
            WakeLockService.stopService(appContext)
            // Nếu bạn muốn xóa cả thông báo từ NotiService:
            // context.stopService(Intent(context, NotiService::class.java))
        }
    }
}
