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

    @JvmStatic
    fun isDarkMode(): Boolean {
        return themeMode.isDarkMode
    }

    /**
     * Áp dụng theme dựa trên cấu hình từ ThemeConfig
     */
    fun switchTheme(activity: Activity? = null): ThemeMode {
        if (activity == null) return themeMode

        val config = ThemeConfig(activity)
        val customMode = config.getThemeMode()

        val wallpaperDrawable = try {
            val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
            if (customWallpaperFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(customWallpaperFile.absolutePath)
                if (bitmap != null) BitmapDrawable(activity.resources, bitmap)
                else WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
            } else {
                WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
            }
        } catch (e: Exception) {
            WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
        }

        val nightModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemNight = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        when (customMode) {
            0 -> { // mặc định: theo hệ thống, không hình nền
                themeMode.isDarkMode = isSystemNight
                applyAppTheme(activity, isSystemNight, false)
            }
            1 -> { // ép sáng
                themeMode.isDarkMode = false
                applyAppTheme(activity, false, false)
            }
            2 -> { // ép tối
                themeMode.isDarkMode = true
                applyAppTheme(activity, true, false)
            }
            3 -> { // hình nền + theo hệ thống
                themeMode.isDarkMode = isSystemNight
                applyAppTheme(activity, isSystemNight, true, wallpaperDrawable)
            }
            4 -> { // hình nền + tối
                themeMode.isDarkMode = true
                applyAppTheme(activity, true, true, wallpaperDrawable)
            }
            5 -> { // hình nền + sáng
                themeMode.isDarkMode = false
                applyAppTheme(activity, false, true, wallpaperDrawable)
            }
            else -> { // fallback
                themeMode.isDarkMode = isSystemNight
                applyAppTheme(activity, isSystemNight, false)
            }
        }

        configureSystemBars(activity)
        return themeMode
    }

    /**
     * Áp dụng theme thực tế cho Activity
     */
    private fun applyAppTheme(activity: Activity, darkMode: Boolean, useWallpaper: Boolean, wallpaper: BitmapDrawable? = null) {
        themeMode.isDarkMode = darkMode

        when {
            useWallpaper && wallpaper != null -> {
                activity.setTheme(if (darkMode) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
                activity.window.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    setBackgroundDrawable(wallpaper)
                }
            }
            darkMode -> {
                activity.setTheme(R.style.AppThemeDark)
                themeMode.isLightStatusBar = false
            }
            else -> {
                activity.setTheme(R.style.AppTheme)
                themeMode.isLightStatusBar = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            }
        }
    }

    /**
     * Thiết lập status bar & navigation bar
     */
    private fun configureSystemBars(activity: Activity) {
        val window = activity.window
        if (!themeMode.isDarkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
            )
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    /**
     * Thay đổi Dark Mode thủ công
     */
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

    /**
     * Lấy trạng thái theme hiện tại
     */
    fun getThemeMode(): ThemeMode = themeMode
}