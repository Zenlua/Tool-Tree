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
        val position: BannerPosition,
        val icon: String?
    )

    private val queue = ArrayDeque<BannerRequest>()
    private var isShowing = false

    /**
     * @param icon Tên resource drawable/mipmap tùy chỉnh (vd "ic_my_icon"). Nếu bỏ trống hoặc
     * không tìm thấy resource tương ứng, mặc định dùng icon của chính app.
     * @param onNoActivity gọi khi không có Activity nào đang foreground (app ở background),
     * dùng để nơi gọi có thể fallback sang Toast thường hoặc bỏ qua.
     */
    fun show(
        title: String? = null,
        message: String,
        type: BannerType = BannerType.INFO,
        position: BannerPosition = BannerPosition.BOTTOM,
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

        val toast = Toast(activity.applicationContext)
        toast.duration = Toast.LENGTH_LONG
        @Suppress("DEPRECATION")
        toast.view = view

        when (req.position) {
            BannerPosition.TOP -> toast.setGravity(
                Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, (62 * density).toInt()
            )
            BannerPosition.BOTTOM -> toast.setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (81 * density).toInt()
            )
        }
        toast.show()

        // Toast tự ẩn theo thời lượng hệ thống quy định (LENGTH_LONG), chỉ cần đợi tương ứng
        // rồi xử lý banner tiếp theo trong hàng đợi (nếu có).
        mainHandler.postDelayed({ showNext() }, TOAST_LONG_DURATION_MS)
    }

    /**
     * Tìm drawable cho icon theo thứ tự ưu tiên:
     * 1. Nếu `name` là đường dẫn file ảnh tồn tại trên máy (vd "/sdcard/.../icon.png") -> decode trực tiếp từ file.
     * 2. Nếu `name` là tên resource drawable/mipmap có sẵn trong app -> dùng resource đó.
     * 3. Nếu không có gì khớp -> mặc định trả về icon của chính app.
     */
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