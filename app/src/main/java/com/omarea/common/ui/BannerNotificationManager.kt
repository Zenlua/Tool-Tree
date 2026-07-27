package com.omarea.common.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tool.tree.R

enum class BannerType { INFO, SUCCESS, WARNING, ERROR }

/**
 * Hiện 1 banner thông báo đè lên trên cùng của Activity (hoặc Dialog) đang hiển thị.
 * Khác với Toast: hiển thị trong nội dung ứng dụng, có thể tùy biến màu/icon/tiêu đề,
 * và chỉ hoạt động khi app đang mở (cần 1 Activity foreground để add vào).
 *
 * Nếu đang có Dialog mở (đăng ký qua [CurrentDialogHolder], ví dụ [ProgressBarDialog]),
 * banner sẽ được add vào chính Window của Dialog đó để chắc chắn nổi lên trên — vì Dialog
 * là 1 Window riêng biệt luôn nổi trên Window của Activity, add vào content Activity sẽ bị
 * Dialog che mất bất kể elevation. Nếu không có Dialog nào mở, banner add vào content
 * của Activity như bình thường.
 *
 * Gọi từ shell: am broadcast -a <applicationId>.broadcast.BANNER --es text "..." --es type "success"
 */
object BannerNotificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    private data class BannerRequest(
        val title: String?,
        val message: String,
        val type: BannerType,
        val durationMs: Long
    )

    private val queue = ArrayDeque<BannerRequest>()
    private var isShowing = false

    /**
     * @param onNoActivity gọi khi không có Activity nào đang foreground (app ở background),
     * dùng để nơi gọi có thể fallback sang Toast hoặc bỏ qua.
     */
    fun show(
        title: String? = null,
        message: String,
        type: BannerType = BannerType.INFO,
        durationMs: Long = 3000L,
        onNoActivity: (() -> Unit)? = null
    ) {
        if (CurrentActivityHolder.get() == null) {
            onNoActivity?.invoke()
            return
        }
        mainHandler.post {
            queue.addLast(BannerRequest(title, message, type, durationMs))
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
            // Không còn Activity nào để hiện -> bỏ qua item này, thử item tiếp theo
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

    /** Tìm ViewGroup gốc để add banner vào: ưu tiên Window của Dialog đang mở, nếu không có thì dùng content của Activity. */
    private fun resolveRoot(activity: Activity): ViewGroup? {
        val topDialog = CurrentDialogHolder.getTopVisible()
        val dialogWindow = topDialog?.window
        if (dialogWindow != null && dialogWindow.decorView.isAttachedToWindow) {
            val dialogContent = dialogWindow.findViewById<ViewGroup>(android.R.id.content)
            if (dialogContent != null) return dialogContent
        }
        return activity.findViewById(android.R.id.content)
    }

    private fun showOn(activity: Activity, req: BannerRequest) {
        val root = resolveRoot(activity)
        if (root == null) {
            showNext()
            return
        }
        val view = LayoutInflater.from(activity).inflate(R.layout.banner_notification, root, false)

        val bannerRoot = view.findViewById<View>(R.id.banner_root)
        val icon = view.findViewById<ImageView>(R.id.banner_icon)
        val titleView = view.findViewById<TextView>(R.id.banner_title)
        val messageView = view.findViewById<TextView>(R.id.banner_message)
        val closeButton = view.findViewById<View>(R.id.banner_close)

        val (colorRes, iconRes) = when (req.type) {
            BannerType.INFO -> R.color.banner_info to R.drawable.ic_banner_info
            BannerType.SUCCESS -> R.color.banner_success to R.drawable.ic_banner_success
            BannerType.WARNING -> R.color.banner_warning to R.drawable.ic_banner_warning
            BannerType.ERROR -> R.color.banner_error to R.drawable.ic_banner_error
        }
        (bannerRoot.background?.mutate() as? GradientDrawable)?.setColor(
            activity.resources.getColor(colorRes, activity.theme)
        )
        icon.setImageResource(iconRes)

        if (!req.title.isNullOrEmpty()) {
            titleView.text = req.title
            titleView.visibility = View.VISIBLE
        } else {
            titleView.visibility = View.GONE
        }
        messageView.text = req.message

        // Tránh banner bị che bởi status bar / notch
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }

        root.addView(view)
        ViewCompat.requestApplyInsets(view)

        var dismissed = false
        val dismiss = {
            if (!dismissed) {
                dismissed = true
                mainHandler.removeCallbacksAndMessages(view)
                view.animate()
                    .translationY(-(view.height.takeIf { it > 0 } ?: 300).toFloat())
                    .alpha(0f)
                    .setDuration(200)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            try {
                                root.removeView(view)
                            } catch (e: Exception) {
                                // root (vd. Window của Dialog) có thể đã bị đóng -> bỏ qua
                            }
                            showNext()
                        }
                    })
                    .start()
            }
        }

        closeButton.setOnClickListener { dismiss() }
        view.setOnClickListener { dismiss() }

        view.alpha = 0f
        view.translationY = -200f
        view.post {
            view.translationY = -(view.height.takeIf { it > 0 } ?: 300).toFloat()
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .start()
        }

        val runnable = Runnable { dismiss() }
        mainHandler.postAtTime(runnable, view, android.os.SystemClock.uptimeMillis() + req.durationMs)
    }
}
