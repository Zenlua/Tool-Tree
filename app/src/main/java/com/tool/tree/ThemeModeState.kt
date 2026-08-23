package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.omarea.common.ui.ThemeMode
import com.omarea.common.ui.BlurEngine
import com.omarea.krscript.executor.ScriptEnvironmen
import java.io.File

object ThemeModeState {

    private var themeMode: ThemeMode = ThemeMode()

    @JvmStatic
    fun isDarkMode(): Boolean = themeMode.isDarkMode

    private fun isBlurDisabled(context: Context): Boolean {
        val file = File(context.filesDir, "home/usr/log/dissblur")
        return try {
            if (file.exists()) {
                file.readText().trim() == "1"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Nơi duy nhất kiểm tra tệp directbg
     */
    @JvmStatic
    fun isDirectBgEnabled(context: Context): Boolean {
        val file = File(context.filesDir, "home/usr/log/directbg")
        return try {
            if (file.exists()) {
                file.readText().trim() == "1"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lấy Drawable ảnh nền (Custom wallpaper -> System wallpaper -> Blur bitmap)
     */
    @JvmStatic
    fun getWallpaperDrawable(context: Context): Drawable? {
        val customWallpaperFile = File(context.filesDir, "home/etc/wallpaper.jpg")
        if (customWallpaperFile.exists()) {
            try {
                val drawable = Drawable.createFromPath(customWallpaperFile.absolutePath)
                if (drawable != null) return drawable
            } catch (_: Exception) {}
        }

        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val sysDrawable = wallpaperManager.drawable ?: wallpaperManager.fastDrawable
            if (sysDrawable != null) return sysDrawable
        } catch (_: Exception) {}

        if (BlurEngine.blurBitmap != null && !BlurEngine.blurBitmap!!.isRecycled) {
            return BitmapDrawable(context.resources, BlurEngine.blurBitmap)
        }

        return null
    }

    fun switchTheme(activity: Activity, themeLevel: Int? = null): ThemeMode {
        val level = themeLevel ?: ThemeConfig(activity).getThemeMode()
        
        val isSystemNight = (activity.resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        val directBg = isDirectBgEnabled(activity)

        when (level.coerceIn(0, 5)) {
            0 -> {
                themeMode.isDarkMode = isSystemNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                activity.setTheme(if (isSystemNight) R.style.AppThemeDark else R.style.AppTheme)
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
                themeMode.isDarkMode = isSystemNight
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                applyWallpaperMode(activity, isSystemNight, directBg)
            }
            4 -> {
                themeMode.isDarkMode = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                applyWallpaperMode(activity, true, directBg)
            }
            5 -> {
                themeMode.isDarkMode = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                applyWallpaperMode(activity, false, directBg)
            }
        }

        ScriptEnvironmen.updateDarkMode(activity, themeMode.isDarkMode)

        BlurEngine.isDirectBgMode = (level >= 3 && directBg && !isBlurDisabled(activity))
        
        if (level >= 3 && !isBlurDisabled(activity)) {
            BlurEngine.isPaused = false
            BlurEngine.blurBitmap = null

            activity.window.decorView.post {
                val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")
                val wallpaperManager = WallpaperManager.getInstance(activity)
                val isLiveWallpaper = (wallpaperManager.wallpaperInfo != null) && !customWallpaperFile.exists()

                if (BlurEngine.isDirectBgMode || isLiveWallpaper) {
                    BlurEngine.directBgColor = ContextCompat.getColor(
                        activity,
                        if (themeMode.isDarkMode) R.color.window_bg_dark else R.color.window_bg_light
                    )
                    BlurEngine.controller.captureBackground(activity)
                } else {
                    BlurEngine.controller.captureAndBlur(activity)
                }
            }
        } else {
            BlurEngine.isPaused = true
        }

        applyWindowFlags(activity)
        return themeMode
    }

    private fun applyWallpaperMode(activity: Activity, isNight: Boolean, directBg: Boolean = false) {
        activity.setTheme(if (isNight) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
        val window = activity.window

        window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        if (directBg) {
            val bgRes = if (isNight) R.color.window_bg_dark else R.color.window_bg_light
            window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(activity, bgRes)))
        } else {
            val wallpaper = WallpaperManager.getInstance(activity)
            val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")

            try {
                if (customWallpaperFile.exists()) {
                    val drawable = Drawable.createFromPath(customWallpaperFile.absolutePath)
                    window.setBackgroundDrawable(drawable)
                } else if (wallpaper.wallpaperInfo != null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(null)
                } else {
                    val sysDrawable = wallpaper.drawable
                    if (sysDrawable != null) {
                        window.setBackgroundDrawable(sysDrawable)
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    }
                }
            } catch (e: Exception) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowFlags(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val rootView = window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            activity.findViewById<View>(R.id.blur_top_container)?.setPadding(0, systemBars.top, 0, 0)
            activity.findViewById<View>(R.id.main_list)?.setPadding(0, systemBars.top, 0, 0)
            activity.findViewById<View>(R.id.blur_bottom_container)?.setPadding(0, 0, 0, systemBars.bottom)
            activity.findViewById<View>(R.id.kr_online_webview)?.setPadding(0, systemBars.top, 0, 0)
            
            insets
        }

        val controller = WindowInsetsControllerCompat(window, rootView)
        val useLightIcons = !themeMode.isDarkMode
        controller.isAppearanceLightStatusBars = useLightIcons
        controller.isAppearanceLightNavigationBars = useLightIcons
    }

    fun getThemeMode(): ThemeMode = themeMode
}
