package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
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

    /**
     * Nếu themeLevel = null thì tự lấy từ ThemeConfig
     */
    fun switchTheme(activity: Activity, themeLevel: Int? = null): ThemeMode {
        val level = themeLevel ?: ThemeConfig(activity).getThemeMode()
        val wallpaper = WallpaperManager.getInstance(activity)
        val wallpaperInfo = wallpaper.wallpaperInfo

        when (level.coerceIn(0, 5)) {
            0 -> { // System default
                val nightModeFlags = activity.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isNight = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeMode.isDarkMode = isNight
                themeMode.isLightStatusBar = !isNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.setTheme(if (isNight) R.style.AppThemeDark else R.style.AppTheme)
            }
            1 -> { // Light
                themeMode.isDarkMode = false
                themeMode.isLightStatusBar = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.AppTheme)
            }
            2 -> { // Dark
                themeMode.isDarkMode = true
                themeMode.isLightStatusBar = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppThemeDark)
            }
            3 -> { // Wallpaper system
                val night = activity.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeMode.isDarkMode = night
                themeMode.isLightStatusBar = !night
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.setTheme(if (night) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
                applyWallpaper(activity, wallpaperInfo, wallpaper)
            }
            4 -> { // Wallpaper dark
                themeMode.isDarkMode = true
                themeMode.isLightStatusBar = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppThemeWallpaper)
                applyWallpaper(activity, wallpaperInfo, wallpaper)
            }
            5 -> { // Wallpaper light
                themeMode.isDarkMode = false
                themeMode.isLightStatusBar = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.AppThemeWallpaperLight)
                applyWallpaper(activity, wallpaperInfo, wallpaper)
            }
        }

        applyWindowFlags(activity)
        return themeMode
    }

    private fun applyWallpaper(activity: Activity, wallpaperInfo: android.app.WallpaperInfo?, wallpaper: WallpaperManager) {
        if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        } else {
            activity.window.setBackgroundDrawable(wallpaper.drawable)
        }
    }

    private fun applyWindowFlags(activity: Activity) {
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

    fun getThemeMode(): ThemeMode = themeMode
}