package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.omarea.common.ui.ThemeMode
import java.io.File

@Suppress("DEPRECATION")
object ThemeModeState {

    private var themeMode: ThemeMode = ThemeMode()

    fun isDarkMode(): Boolean = themeMode.isDarkMode

    fun switchTheme(activity: Activity? = null): ThemeMode {
        if (activity == null) return themeMode

        val config = ThemeConfig(activity)
        val customMode = config.getThemeMode()
        val wallpaperManager = WallpaperManager.getInstance(activity)

        // Lấy wallpaper tĩnh nếu có file
        val wallpaperDrawable: BitmapDrawable? = try {
            val file = File(activity.filesDir, "home/etc/wallpaper.jpg")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap?.let { BitmapDrawable(activity.resources, it) }
            } else null
        } catch (e: Exception) { null }

        // Kiểm tra chế độ night hệ thống
        val nightModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemNight = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        when (customMode) {
            0 -> applyTheme(activity, isSystemNight, false, wallpaperDrawable, wallpaperManager)
            1 -> applyTheme(activity, false, false, wallpaperDrawable, wallpaperManager)
            2 -> applyTheme(activity, true, false, wallpaperDrawable, wallpaperManager)
            3 -> applyTheme(activity, isSystemNight, true, wallpaperDrawable, wallpaperManager)
            4 -> applyTheme(activity, true, true, wallpaperDrawable, wallpaperManager)
            5 -> applyTheme(activity, false, true, wallpaperDrawable, wallpaperManager)
            else -> applyTheme(activity, isSystemNight, false, wallpaperDrawable, wallpaperManager)
        }

        configureSystemBars(activity)
        return themeMode
    }

    private fun applyTheme(
        activity: Activity,
        darkMode: Boolean,
        useWallpaper: Boolean,
        wallpaper: BitmapDrawable?,
        wallpaperManager: WallpaperManager
    ) {
        themeMode.isDarkMode = darkMode
        val window = activity.window

        // Reset flags giống code cũ
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        if (useWallpaper) {
            val info = wallpaperManager.wallpaperInfo
            if (info != null && info.packageName != null) {
                // Live wallpaper
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            } else if (wallpaper != null) {
                // Wallpaper tĩnh
                window.setBackgroundDrawable(wallpaper)
            }

            activity.setTheme(if (darkMode) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
        } else if (darkMode) {
            activity.setTheme(R.style.AppThemeDark)
            // Không set màu nền mặc định
        } else {
            activity.setTheme(R.style.AppTheme)
            // Không set màu nền mặc định
        }

        themeMode.isLightStatusBar = !darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun configureSystemBars(activity: Activity) {
        val window = activity.window
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        if (themeMode.isLightStatusBar) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        window.decorView.systemUiVisibility = flags
    }

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