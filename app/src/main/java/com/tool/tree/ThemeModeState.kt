package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.omarea.common.ui.ThemeMode
import java.io.File
import com.omarea.common.ui.BlurEngine

@Suppress("DEPRECATION")
object ThemeModeState {

    private var themeMode: ThemeMode = ThemeMode()

    @JvmStatic
    fun isDarkMode(): Boolean = themeMode.isDarkMode

    fun switchTheme(activity: Activity, themeLevel: Int? = null): ThemeMode {
        val level = themeLevel ?: ThemeConfig(activity).getThemeMode()
        val wallpaper = WallpaperManager.getInstance(activity)
        val wallpaperInfo = wallpaper.wallpaperInfo

        // File wallpaper tùy chỉnh
        val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
        val useCustomWallpaper = customWallpaperFile.exists()

        when (level.coerceIn(0, 5)) {
            0 -> { // System default
                val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
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
                val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isNight = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeMode.isDarkMode = isNight
                themeMode.isLightStatusBar = !isNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper)
            }
            4 -> { // Wallpaper dark
                themeMode.isDarkMode = true
                themeMode.isLightStatusBar = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper, true)
            }
            5 -> { // Wallpaper light
                themeMode.isDarkMode = false
                themeMode.isLightStatusBar = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper, false)
            }
        }

        if (level >= 3) {
            BlurEngine.isPaused = false
            BlurEngine.controller.captureAndBlur(activity)
        } else {
            BlurEngine.isPaused = true
            BlurEngine.blurBitmap = null
        }

        applyWindowFlags(activity)
        return themeMode
    }

    private fun applyWallpaperMode(
        activity: Activity,
        wallpaper: WallpaperManager,
        wallpaperInfo: android.app.WallpaperInfo?,
        useCustomWallpaper: Boolean,
        forceDark: Boolean? = null
    ) {
        val night = forceDark ?: (activity.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES)

        themeMode.isDarkMode = night
        themeMode.isLightStatusBar = !night

        activity.setTheme(if (night) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)

        try {
            val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
            if (useCustomWallpaper && customWallpaperFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(customWallpaperFile.absolutePath)
                activity.window.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(activity.resources, bitmap))
            } else if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            } else {
                activity.window.setBackgroundDrawable(wallpaper.drawable)
            }
        } catch (e: Exception) {
            // fallback an toàn
            if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            } else {
                activity.window.setBackgroundDrawable(wallpaper.drawable)
            }
        }
    }

    private fun applyWindowFlags(activity: Activity) {
        if (!themeMode.isDarkMode && themeMode.isLightStatusBar) {
            activity.window.run {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                val flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                decorView.systemUiVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags
                }
            }
        }
    }

    fun getThemeMode(): ThemeMode = themeMode
}