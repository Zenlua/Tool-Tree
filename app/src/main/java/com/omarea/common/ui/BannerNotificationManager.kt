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
 * tự add view vào content của Activity/Dialog. Lý do: Dialog là 1 Window riêng, kích thước
 * chỉ vừa đủ khung dialog (không phải full màn hình), nên add view thường vào bên trong nó
 * sẽ bị bó hẹp/cắt theo đúng khung dialog nhỏ đó. TYPE_TOAST là loại cửa sổ đặc biệt của hệ
 * thống luôn nổi trên mọi Dialog/Activity của app (đây cũng chính là lý do ToastReceiver có
 * sẵn của bạn luôn hiện được dù đang có dialog hay không).
 *
 * Đánh đổi: cửa sổ Toast KHÔNG nhận sự kiện chạm (không bấm tắt sớm được), và không thể tùy
 * chỉnh thời gian hiển thị (Android giới hạn cứng ở mức LENGTH_LONG ~3.5s cho mọi Toast).
 *
 * Gọi từ shell: am broadcast -a <applicationId>.broadcast.BANNER --es text "..." --es type "success" --es position "bottom"
 */
object BannerNotificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    // Thời gian hiển thị thực tế của Toast.LENGTH_LONG do hệ thống quy định, dùng để giãn cách
    // các banner trong hàng đợi, tránh cái sau ghi đè ngay lên cái trước.
    private const val TOAST_LONG_DURATION_MS = 3500L

    private data class BannerRequest(
        val title: String?,
        val message: String,
        val type: BannerType,
        val position: BannerPosition
    )

    private val queue = ArrayDeque<BannerRequest>()
    private var isShowing = false

    /**
     * @param onNoActivity gọi khi không có Activity nào đang foreground (app ở background),
     * dùng để nơi gọi có thể fallback sang Toast thường hoặc bỏ qua.
     */
    fun show(
        title: String? = null,
        message: String,
        type: BannerType = BannerType.INFO,
        position: BannerPosition = BannerPosition.TOP,
        onNoActivity: (() -> Unit)? = null
    ) {
        if (CurrentActivityHolder.get() == null) {
            onNoActivity?.invoke()
            return
        }
        mainHandler.post {
            queue.addLast(BannerRequest(title, message, type, position))
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

        // Toast luôn tự set kích thước cửa sổ kiểu wrap_content theo nội dung, gán layoutParams
        // cho view KHÔNG có tác dụng ép độ rộng (window cha quyết định MeasureSpec, không phải
        // layoutParams của view con). Phải dùng minimumWidth để ép độ rộng tối thiểu khi tự đo.
        val density = activity.resources.displayMetrics.density
        val marginPx = (12 * density).toInt()
        val screenWidth = activity.resources.displayMetrics.widthPixels
        view.minimumWidth = screenWidth - marginPx * 2

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_LONG
        @Suppress("DEPRECATION")
        toast.view = view

        val offsetPx = (24 * density).toInt()
        when (req.position) {
            BannerPosition.TOP -> toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, offsetPx)
            BannerPosition.BOTTOM -> toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, offsetPx)
        }
        toast.show()

        // Toast tự ẩn theo thời lượng hệ thống quy định (LENGTH_LONG), chỉ cần đợi tương ứng
        // rồi xử lý banner tiếp theo trong hàng đợi (nếu có).
        mainHandler.postDelayed({ showNext() }, TOAST_LONG_DURATION_MS)
    }
}