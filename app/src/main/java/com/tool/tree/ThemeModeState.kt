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

    private var themeMode = ThemeMode()

    @JvmStatic
    fun isDarkMode(): Boolean = themeMode.isDarkMode

    fun switchTheme(activity: Activity? = null): ThemeMode {
        if (activity == null) return themeMode

        val config = ThemeConfig(activity)
        val customMode = config.getThemeMode()
        val nightModeFlags = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isSystemNight = nightModeFlags == Configuration.UI_MODE_NIGHT_YES

        val wallpaperDrawable = loadWallpaper(activity)

        when (customMode) {
            0 -> applyTheme(activity, isSystemNight, false, null)             // theo hệ thống
            1 -> applyTheme(activity, false, false, null)                     // ép sáng
            2 -> applyTheme(activity, true, false, null)                      // ép tối
            3 -> applyTheme(activity, isSystemNight, true, wallpaperDrawable) // wallpaper + theo hệ thống
            4 -> applyTheme(activity, true, true, wallpaperDrawable)          // wallpaper + tối
            5 -> applyTheme(activity, false, true, wallpaperDrawable)         // wallpaper + sáng
            else -> applyTheme(activity, isSystemNight, false, null)          // fallback
        }

        return themeMode
    }

    private fun loadWallpaper(activity: Activity): BitmapDrawable? {
        return try {
            val file = File(activity.filesDir, "home/etc/wallpaper.jpg")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                bitmap?.let { BitmapDrawable(activity.resources, it) }
                    ?: WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
            } else {
                WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
            }
        } catch (e: Exception) {
            WallpaperManager.getInstance(activity).drawable as? BitmapDrawable
        }
    }

    private fun applyTheme(activity: Activity, darkMode: Boolean, useWallpaper: Boolean, wallpaper: BitmapDrawable?) {
        themeMode.isDarkMode = darkMode
        val wm = WallpaperManager.getInstance(activity)

        activity.window.run {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

            when {
                useWallpaper && wallpaper != null -> {
                    activity.setTheme(if (darkMode) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)

                    if (wm.wallpaperInfo != null && wm.wallpaperInfo.packageName != null) {
                        addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    } else {
                        setBackgroundDrawable(wallpaper)
                    }
                }
                darkMode -> {
                    activity.setTheme(R.style.AppThemeDark)
                    themeMode.isLightStatusBar = false
                }
                else -> {
                    activity.setTheme(R.style.AppTheme)
                    themeMode.isLightStatusBar = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                }
            }

            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            decorView.systemUiVisibility = if (!themeMode.isDarkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                flags
            } else {
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
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