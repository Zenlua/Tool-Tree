package com.omarea.common.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.tool.tree.R

enum class BannerType { INFO, SUCCESS, WARNING, ERROR }
enum class BannerPosition { TOP, BOTTOM }

/**
 * Hiện 1 banner thông báo đè lên trên cùng của Activity đang foreground -- KỂ CẢ khi
 * đang có Dialog mở (ví dụ ProgressBarDialog).
 *
 * Kỹ thuật: dùng chính cơ chế của Toast (custom view Toast, type cửa sổ TYPE_TOAST) thay vì
 * tự add view vào content của Activity/Dialog.
 */
object BannerNotificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())
    private const val TOAST_LONG_DURATION_MS = 3500L

    private data class BannerRequest(
        val title: String?,
        val message: String,
        val type: BannerType,
        val position: BannerPosition,
        val icon: String?
    )

    private val queue = ArrayDeque<BannerRequest>()
    private var isShowing = false

    fun show(
        title: String? = null,
        message: String,
        type: BannerType = BannerType.INFO,
        position: BannerPosition = BannerPosition.TOP,
        icon: String? = null,
        onNoActivity: (() -> Unit)? = null
    ) {
        if (CurrentActivityHolder.get() == null) {
            onNoActivity?.invoke()
            return
        }
        mainHandler.post {
            queue.addLast(BannerRequest(title, message, type, position, icon))
            if (!isShowing) showNext()
        }
    }

    private fun showNext() {
        val req = queue.removeFirstOrNull()
        if (req == null) {
            isShowing = false
            return
        }
        val activity = CurrentActivityHolder.get()
        if (activity == null) {
            showNext()
            return
        }
        isShowing = true
        try {
            showOn(activity, req)
        } catch (e: Exception) {
            showNext()
        }
    }

    private fun showOn(activity: Activity, req: BannerRequest) {
        val view = LayoutInflater.from(activity).inflate(R.layout.banner_notification, null, false)

        val bannerRoot = view.findViewById<View>(R.id.banner_root)
        val icon = view.findViewById<ImageView>(R.id.banner_icon)
        val titleView = view.findViewById<TextView>(R.id.banner_title)
        val messageView = view.findViewById<TextView>(R.id.banner_message)

        val colorRes = when (req.type) {
            BannerType.INFO -> R.color.banner_info
            BannerType.SUCCESS -> R.color.banner_success
            BannerType.WARNING -> R.color.banner_warning
            BannerType.ERROR -> R.color.banner_error
        }
        (bannerRoot.background?.mutate() as? GradientDrawable)?.setColor(
            activity.resources.getColor(colorRes, activity.theme)
        )
        icon.setImageDrawable(resolveIcon(activity, req.icon))

        if (!req.title.isNullOrEmpty()) {
            titleView.text = req.title
            titleView.visibility = View.VISIBLE
        } else {
            titleView.visibility = View.GONE
        }
        messageView.text = req.message

        val density = activity.resources.displayMetrics.density
        val yOffset = (50 * density).toInt()

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_LONG
        @Suppress("DEPRECATION")
        toast.view = view

        // Xác định gravity gọn gàng, tránh lỗi ngắt dòng khi gọi hàm
        val gravity = when (req.position) {
            BannerPosition.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            BannerPosition.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
        toast.setGravity(gravity, 0, yOffset)

        toast.show()

        mainHandler.postDelayed({ showNext() }, TOAST_LONG_DURATION_MS)
    }

    private fun resolveIcon(activity: Activity, name: String?): android.graphics.drawable.Drawable? {
        if (!name.isNullOrEmpty()) {
            if (name.startsWith("/") || name.startsWith("file://")) {
                try {
                    val path = name.removePrefix("file://")
                    val file = java.io.File(path)
                    if (file.exists() && file.isFile) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            return android.graphics.drawable.BitmapDrawable(activity.resources, bitmap)
                        }
                    }
                } catch (e: Exception) {
                }
            } else {
                try {
                    var resId = activity.resources.getIdentifier(name, "drawable", activity.packageName)
                    if (resId == 0) {
                        resId = activity.resources.getIdentifier(name, "mipmap", activity.packageName)
                    }
                    if (resId != 0) {
                        return androidx.core.content.ContextCompat.getDrawable(activity, resId)
                    }
                } catch (e: Exception) {
                }
            }
        }
        return try {
            activity.packageManager.getApplicationIcon(activity.packageName)
        } catch (e: Exception) {
            null
        }
    }
}
