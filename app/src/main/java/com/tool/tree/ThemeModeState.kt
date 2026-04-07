package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.omarea.common.ui.ThemeMode

@Suppress("DEPRECATION")
object ThemeModeState {
    private var themeMode: ThemeMode = ThemeMode()

    @JvmStatic
    fun isDarkMode(): Boolean = themeMode.isDarkMode

    // themeType: 0 = mặc định, 1 = hình nền
    fun switchTheme(activity: Activity? = null, themeType: Int = 0): ThemeMode {
        if (activity != null) {
            val nightModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isNightMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            themeMode.isDarkMode = isNightMode

            when (themeType) {
                1 -> { // hình nền
                    val wallpaper = WallpaperManager.getInstance(activity)
                    val wallpaperInfo = wallpaper.wallpaperInfo

                    if (isNightMode) {
                        activity.setTheme(R.style.AppThemeWallpaper)
                    } else {
                        activity.setTheme(R.style.AppThemeWallpaperLight)
                    }

                    if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    } else {
                        activity.window.setBackgroundDrawable(wallpaper.drawable)
                    }
                }
                else -> { // mặc định
                    if (isNightMode) {
                        activity.setTheme(R.style.AppThemeDark)
                        themeMode.isLightStatusBar = false
                    } else {
                        activity.setTheme(R.style.AppTheme)
                        themeMode.isLightStatusBar = true
                    }
                }
            }

            // Cấu hình status bar / navigation bar cho light mode
            if (!themeMode.isDarkMode && themeMode.isLightStatusBar) {
                activity.window.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                    decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    } else {
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
            }
        }
        return themeMode
    }

    // Thay đổi Dark Mode thủ công
    fun setNightModeManually(isDarkMode: Boolean) {
        themeMode.isDarkMode = isDarkMode
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            themeMode.isLightStatusBar = false
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            themeMode.isLightStatusBar = true
        }
    }

    fun getThemeMode(): ThemeMode = themeMode
}