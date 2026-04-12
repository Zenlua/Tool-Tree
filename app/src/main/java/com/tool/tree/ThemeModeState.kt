package com.tool.tree

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.omarea.common.ui.ThemeMode
import com.omarea.common.ui.BlurEngine
import java.io.File

object ThemeModeState {

    private var themeMode: ThemeMode = ThemeMode()

    @JvmStatic
    fun isDarkMode(): Boolean = themeMode.isDarkMode

    fun switchTheme(activity: Activity, themeLevel: Int? = null): ThemeMode {
        val level = themeLevel ?: ThemeConfig(activity).getThemeMode()
        
        // Cập nhật logic xác định Night Mode hệ thống nhanh hơn
        val isSystemNight = (activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

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
                applyWallpaperMode(activity, isSystemNight)
            }
            4 -> {
                themeMode.isDarkMode = true
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                applyWallpaperMode(activity, true)
            }
            5 -> {
                themeMode.isDarkMode = false
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                applyWallpaperMode(activity, false)
            }
        }

        // BlurEngine logic
        if (level >= 3) {
            BlurEngine.isPaused = false
            // Đảm bảo View đã layout xong trước khi capture
            activity.window.decorView.post {
                BlurEngine.controller.captureAndBlur(activity)
            }
        } else {
            BlurEngine.isPaused = true
        }

        applyWindowFlags(activity)
        return themeMode
    }

    private fun applyWallpaperMode(activity: Activity, isNight: Boolean) {
        activity.setTheme(if (isNight) R.style.AppThemeWallpaper else R.style.AppThemeWallpaperLight)
        
        val wallpaper = WallpaperManager.getInstance(activity)
        val customWallpaperFile = File(activity.filesDir, "home/etc/wallpaper.jpg")

        try {
            if (customWallpaperFile.exists()) {
                // Sử dụng createFromPath để tiết kiệm bộ nhớ hơn decode trực tiếp
                val drawable = Drawable.createFromPath(customWallpaperFile.absolutePath)
                activity.window.setBackgroundDrawable(drawable)
            } else if (wallpaper.wallpaperInfo != null) {
                // Nếu là Live Wallpaper, dùng flag SHOW_WALLPAPER
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                activity.window.setBackgroundDrawable(null)
            } else {
                // Hình nền tĩnh hệ thống
                activity.window.setBackgroundDrawable(wallpaper.drawable)
            }
        } catch (e: Exception) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        }
    }

    private fun applyWindowFlags(activity: Activity) {
        val window = activity.window
        // Thiết lập Edge-to-Edge chuẩn SDK 23+
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val rootView = window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Cập nhật padding cho các container nếu tồn tại
            activity.findViewById<View>(R.id.blur_top_container)?.setPadding(0, systemBars.top, 0, 0)
            activity.findViewById<View>(R.id.file_selector_list)?.setPadding(0, systemBars.top, 0, 0)
            activity.findViewById<View>(R.id.main_list)?.setPadding(0, systemBars.top, 0, 0)
            activity.findViewById<View>(R.id.blur_bottom_container)?.setPadding(0, 0, 0, systemBars.bottom)
            
            insets // Trả về insets để các view khác vẫn nhận được
        }

        // Điều khiển màu icon trên thanh trạng thái
        val controller = WindowInsetsControllerCompat(window, rootView)
        val useLightIcons = !themeMode.isDarkMode
        controller.isAppearanceLightStatusBars = useLightIcons
        controller.isAppearanceLightNavigationBars = useLightIcons
    }

    fun getThemeMode(): ThemeMode = themeMode
}
