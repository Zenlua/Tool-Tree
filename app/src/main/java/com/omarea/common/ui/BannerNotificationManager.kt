package com.omarea.common.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.tool.tree.R

enum class BannerType { INFO, SUCCESS, WARNING, ERROR }

/**
 * Hiện 1 banner thông báo đè lên trên cùng của Activity đang foreground -- KỂ CẢ khi
 * đang có Dialog mở (ví dụ ProgressBarDialog).
 *
 * Kỹ thuật: dùng chính cơ chế của Toast (custom view Toast, type cửa sổ TYPE_TOAST) thay vì
 * tự add view vào content của Activity/Dialog. Lý do: Dialog là 1 Window riêng, kích thước
 * chỉ vừa đủ khung dialog (không phải full màn hình), nên add view thường vào bên trong nó
 * sẽ bị bó hẹp/cắt theo đúng khung dialog nhỏ đó. TYPE_TOAST là loại cửa sổ đặc biệt của hệ
 * thống luôn nổi trên mọi Dialog/Activity của app (đây cũng chính là lý do ToastReceiver có
 * sẵn của bạn luôn hiện được dù đang có dialog hay không).
 *
 * Đánh đổi: cửa sổ Toast KHÔNG nhận sự kiện chạm, nên banner không thể bấm để tắt sớm --
 * sẽ tự động biến mất sau đúng thời gian `durationMs` được yêu cầu.
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
    private var currentToast: Toast? = null

    /**
     * @param onNoActivity gọi khi không có Activity nào đang foreground (app ở background),
     * dùng để nơi gọi có thể fallback sang Toast thường hoặc bỏ qua.
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

        // Toast mặc định wrap_content theo nội dung -> ép chiều rộng gần bằng màn hình
        // (trừ lề 2 bên) để có dáng banner tràn ngang như mong muốn.
        val density = activity.resources.displayMetrics.density
        val marginPx = (12 * density).toInt()
        val screenWidth = activity.resources.displayMetrics.widthPixels
        view.layoutParams = ViewGroup.LayoutParams(screenWidth - marginPx * 2, ViewGroup.LayoutParams.WRAP_CONTENT)

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_LONG
        @Suppress("DEPRECATION")
        toast.view = view
        // Offset nhỏ để không dính sát mép trên / bị status bar che
        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, (24 * density).toInt())
        toast.show()
        currentToast = toast

        val runnable = Runnable {
            try {
                toast.cancel()
            } catch (e: Exception) {
            }
            currentToast = null
            showNext()
        }
        mainHandler.postDelayed(runnable, req.durationMs)
    }
}
