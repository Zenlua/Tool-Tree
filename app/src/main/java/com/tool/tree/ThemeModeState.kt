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

        val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
        val useCustomWallpaper = customWallpaperFile.exists()

        when (level.coerceIn(0, 5)) {
            0 -> {
                val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isNight = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeMode.isDarkMode = isNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.setTheme(if (isNight) R.style.AppThemeDark else R.style.AppTheme)
            }
            1 -> {
                themeMode.isDarkMode = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppThemeDark)
            }
            2 -> {
                themeMode.isDarkMode = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.AppTheme)
            }
            3 -> {
                val nightModeFlags = activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val isNight = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                themeMode.isDarkMode = isNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper)
            }
            4 -> {
                themeMode.isDarkMode = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper, true)
            }
            5 -> {
                themeMode.isDarkMode = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                applyWallpaperMode(activity, wallpaper, wallpaperInfo, useCustomWallpaper, false)
            }
        }

        // CẬP NHẬT: Luôn gọi captureAndBlur khi ở chế độ Wallpaper (level >= 3)
        // Không sử dụng check null ở đây vì BlurController đã có logic so sánh File Size bên trong
        if (level >= 3) {
            BlurEngine.isPaused = false
            BlurEngine.controller.captureAndBlur(activity)
        } else {
            BlurEngine.isPaused = true
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
        val night = forceDark ?: (activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES)
        themeMode.isDarkMode = night
        activity.setTheme(if (night) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
        
        try {
            if (useCustomWallpaper) {
                val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
                if (customWallpaperFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(customWallpaperFile.absolutePath)
                    activity.window.setBackgroundDrawable(android.graphics.drawable.BitmapDrawable(activity.resources, bitmap))
                }
            } else if (wallpaperInfo != null && wallpaperInfo.packageName != null) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            } else {
                // CẬP NHẬT: Xóa cache của WallpaperManager trước khi lấy Drawable hệ thống
                wallpaper.forgetLoadedWallpaper()
                activity.window.setBackgroundDrawable(wallpaper.drawable)
            }
        } catch (e: Exception) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        }
    }

    private fun applyWindowFlags(activity: Activity) {
        val window = activity.window
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rootView = window.decorView.rootView
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                activity.findViewById<View>(R.id.blur_top_container)?.setPadding(0, systemBars.top, 0, 0)
                activity.findViewById<View>(R.id.file_selector_list)?.setPadding(0, systemBars.top, 0, 0)
                activity.findViewById<View>(R.id.main_list)?.setPadding(0, systemBars.top, 0, 0)
                activity.findViewById<View>(R.id.blur_bottom_container)?.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }
        }

        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        if (!themeMode.isDarkMode) {
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        } else {
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    fun getThemeMode(): ThemeMode = themeMode
}
