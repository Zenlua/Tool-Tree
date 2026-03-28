package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.omarea.common.ui.ThemeMode

@Suppress("DEPRECATION")
object ThemeModeState {
    private var themeMode: ThemeMode = ThemeMode()

    // Hàm này vẫn giữ nguyên tính năng cũ: tự động xác định chế độ Dark/Light mode dựa vào cấu hình hệ thống
    fun switchTheme(activity: Activity? = null): ThemeMode {
        if (activity != null) {
            val nightModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

            // Kiểm tra quyền đọc/ghi bộ nhớ ngoài nếu cần thiết
            if (ThemeConfig(activity).getAllowTransparentUI()) {
                val wallpaper = WallpaperManager.getInstance(activity)
                val wallpaperInfo = wallpaper.wallpaperInfo

                if (isNightMode) {
                    themeMode.isDarkMode = true
                    activity.setTheme(R.style.AppThemeWallpaper)
                } else {
                    themeMode.isDarkMode = false
                    activity.setTheme(R.style.AppThemeWallpaperLight)
                }

                // Xử lý với hình nền động
                if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                } else {
                    val wallpaperDrawable = wallpaper.drawable
                    activity.window.setBackgroundDrawable(wallpaperDrawable)
                }
            } else {
                if (isNightMode) {
                    themeMode.isDarkMode = true
                    themeMode.isLightStatusBar = false
                    activity.setTheme(R.style.AppThemeDark)
                } else {
                    themeMode.isDarkMode = false
                }
            }

            // Thiết lập giao diện cho status bar và navigation bar
            if (!themeMode.isDarkMode) {
                themeMode.isLightStatusBar = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

                activity.window.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
            }
        }
        return themeMode
    }

    // Thêm khả năng thay đổi Dark Mode thủ công mà không ảnh hưởng đến tính năng cũ
    fun setNightModeManually(isDarkMode: Boolean) {
        // Cập nhật lại chế độ dark mode theo lựa chọn của người dùng
        themeMode.isDarkMode = isDarkMode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            themeMode.isLightStatusBar = false
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            themeMode.isLightStatusBar = true
        }
    }

    // Lấy trạng thái chế độ hiện tại (dark/light mode)
    fun getThemeMode(): ThemeMode {
        return themeMode
    }
}
