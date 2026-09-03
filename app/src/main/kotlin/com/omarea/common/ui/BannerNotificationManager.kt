package com.omarea.common.ui

import android.app.Activity
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.omarea.common.shell.ShellExecutor
import com.tool.tree.R

enum class BannerType { INFO, SUCCESS, WARNING, ERROR }
enum class BannerPosition { TOP, BOTTOM }

/**
 * Hiện 1 banner thông báo đè lên trên cùng của Activity đang foreground -- KỂ CẢ khi đang
 * có Dialog mở (ví dụ cửa sổ xem log khi chạy script).
 *
 * Kỹ thuật: add view banner như 1 sub-window riêng bằng WindowManager, gắn vào token cửa sổ
 * của Activity đang foreground (lấy qua CurrentActivityHolder), dùng type
 * TYPE_APPLICATION_ATTACHED_DIALOG -- đúng loại sub-window mà Dialog/AlertDialog cũng dùng,
 * nên add SAU 1 Dialog đang mở thì banner sẽ nổi lên trên Dialog đó. Không cần thêm quyền gì
 * mới (không phải SYSTEM_ALERT_WINDOW, chỉ cần token của chính Activity).
 *
 * Lý do không dùng Toast: Toast KHÔNG nhận sự kiện chạm nên không thể bấm được nút Xác
 * nhận/Hủy bỏ khi banner có kèm script cần chạy.
 *
 * Mọi banner (kể cả không có script) đều tự ẩn sau [countdownSeconds] giây, mặc định 5s.
 *
 * Gọi từ shell: am broadcast -a <applicationId>.broadcast.BANNER --es text "..." --es type "success" --es position "bottom"
 * Gọi kèm script cần xác nhận trước khi chạy:
 *   am broadcast -a <applicationId>.broadcast.BANNER --es text "Cập nhật script mới, chạy ngay?" \
 *       --es script "sh /sdcard/Download/update.sh" --es confirm "Chạy" --es cancel "Bỏ qua" --ei countdown 5
 */
object BannerNotificationManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    // Fallback an toàn nếu countdownSeconds <= 0 (tránh banner treo mãi không tự ẩn), KHÔNG
    // còn là thời lượng chính dùng cho banner nữa (xem countdownSeconds trong show()).
    private const val FALLBACK_DURATION_MS = 3500L


    private data class BannerRequest(
        val title: String?,
        val message: String,
        val type: BannerType,
        val position: BannerPosition,
        val icon: String?,
        val script: String?,
        val confirmText: String?,
        val cancelText: String?,
        val countdownSeconds: Int
    )

    private val queue = ArrayDeque<BannerRequest>()
    private var isShowing = false

    // View banner đang hiện trên màn hình (nếu có) + WindowManager quản lý nó + Runnable đếm
    // ngược đang chạy, dùng để có thể gỡ bỏ đúng lúc khi người dùng bấm nút hoặc hết giờ.
    private var currentView: View? = null
    private var currentWindowManager: WindowManager? = null
    private var countdownRunnable: Runnable? = null

    /**
     * @param icon Tên resource drawable/mipmap tùy chỉnh (vd "ic_my_icon"). Nếu bỏ trống hoặc
     * không tìm thấy resource tương ứng, mặc định dùng icon của chính app.
     * @param script Lệnh/script sẽ được chạy (bằng đúng quyền hiện có của app, KHÔNG tự xin
     * root) khi người dùng bấm nút Xác nhận. Khi khác null/rỗng, banner sẽ tự hiện thêm 2 nút
     * Xác nhận/Hủy bỏ; hết [countdownSeconds] giây mà chưa bấm gì thì coi như Hủy bỏ (KHÔNG
     * chạy script). Bỏ trống (mặc định) thì banner hiện như bình thường, không có nút.
     * @param confirmText / cancelText nhãn tùy chỉnh cho 2 nút (chỉ có ý nghĩa khi [script]
     * khác null/rỗng); bỏ trống thì dùng nhãn mặc định "Xác nhận"/"Hủy bỏ".
     * @param countdownSeconds số giây trước khi banner tự ẩn, mặc định 5 giây. Áp dụng cho CẢ
     * 2 chế độ: có [script] thì hết giờ = tự Hủy bỏ, không có [script] thì hết giờ = tự ẩn
     * banner như bình thường.
     * @param onNoActivity gọi khi không có Activity nào đang foreground (app ở background),
     * dùng để nơi gọi có thể fallback sang Toast thường hoặc bỏ qua.
     */
    fun show(
        title: String? = null,
        message: String,
        type: BannerType = BannerType.INFO,
        position: BannerPosition = BannerPosition.BOTTOM,
        icon: String? = null,
        script: String? = null,
        confirmText: String? = null,
        cancelText: String? = null,
        countdownSeconds: Int = 5,
        onNoActivity: (() -> Unit)? = null
    ) {
        if (CurrentActivityHolder.get() == null) {
            onNoActivity?.invoke()
            return
        }
        mainHandler.post {
            queue.addLast(
                BannerRequest(title, message, type, position, icon, script, confirmText, cancelText, countdownSeconds)
            )
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
        val actionsRow = view.findViewById<View>(R.id.banner_actions)
        val confirmBtn = view.findViewById<TextView>(R.id.banner_btn_confirm)
        val cancelBtn = view.findViewById<TextView>(R.id.banner_btn_cancel)

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
        val windowManager = activity.windowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        // Ưu tiên gắn vào token của window đang thực sự ở trên cùng (vd 1 dialog full-screen
        // kiểu DialogLogFragment, xem TopWindowHolder) -- loại dialog windowIsFloating=false
        // tự tách thành 1 lớp cửa sổ riêng nằm TRÊN cửa sổ Activity, nên nếu vẫn gắn vào token
        // cửa sổ Activity gốc như cũ thì banner sẽ bị dialog đó che mất. Không có dialog nào
        // kiểu này đang mở thì fallback về token cửa sổ Activity như trước (không cần quyền
        // SYSTEM_ALERT_WINDOW trong cả 2 trường hợp).
        val anchorWindow = TopWindowHolder.current() ?: activity.window
        layoutParams.token = anchorWindow.decorView.windowToken
        when (req.position) {
            BannerPosition.TOP -> {
                layoutParams.gravity = Gravity.TOP
                layoutParams.y = (62 * density).toInt()
            }
            BannerPosition.BOTTOM -> {
                layoutParams.gravity = Gravity.BOTTOM
                layoutParams.y = (81 * density).toInt()
            }
        }

        currentView = view
        currentWindowManager = windowManager
        windowManager.addView(view, layoutParams)

        val script = req.script
        if (!script.isNullOrEmpty()) {
            // Có script kèm theo -> hiện 2 nút Xác nhận/Hủy bỏ + đếm ngược, không tự ẩn theo
            // thời lượng cố định như banner thường.
            actionsRow.visibility = View.VISIBLE
            cancelBtn.text = if (!req.cancelText.isNullOrEmpty()) req.cancelText else activity.getString(R.string.kr_banner_cancel)
            val confirmLabel = if (!req.confirmText.isNullOrEmpty()) req.confirmText else activity.getString(R.string.kr_banner_confirm)

            var remaining = if (req.countdownSeconds > 0) req.countdownSeconds else 0
            fun updateConfirmLabel() {
                confirmBtn.text = if (remaining > 0) "$confirmLabel (${remaining}s)" else confirmLabel
            }
            updateConfirmLabel()

            val tick = object : Runnable {
                override fun run() {
                    remaining--
                    if (remaining <= 0) {
                        // Hết giờ mà chưa bấm gì -> tự Hủy bỏ, KHÔNG chạy script.
                        dismissCurrent()
                        showNext()
                    } else {
                        updateConfirmLabel()
                        mainHandler.postDelayed(this, 1000L)
                    }
                }
            }
            countdownRunnable = tick
            if (remaining > 0) {
                mainHandler.postDelayed(tick, 1000L)
            }

            confirmBtn.setOnClickListener {
                dismissCurrent()
                runScript(script)
                showNext()
            }
            cancelBtn.setOnClickListener {
                dismissCurrent()
                showNext()
            }
        } else {
            actionsRow.visibility = View.GONE
            // Không có script -> banner thường, tự ẩn sau [countdownSeconds] giây (mặc định
            // 5s, dùng chung tham số countdown với chế độ có nút Xác nhận/Hủy bỏ).
            val autoDismissMs = if (req.countdownSeconds > 0) req.countdownSeconds * 1000L else FALLBACK_DURATION_MS
            mainHandler.postDelayed({
                dismissCurrent()
                showNext()
            }, autoDismissMs)
        }
    }

    private fun dismissCurrent() {
        countdownRunnable?.let { mainHandler.removeCallbacks(it) }
        countdownRunnable = null
        val view = currentView ?: return
        val windowManager = currentWindowManager
        currentView = null
        currentWindowManager = null
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: Exception) {
            // View có thể đã bị hệ thống tự gỡ trước đó (vd Activity bị destroy) -> bỏ qua.
        }
    }

    /**
     * Chạy [script] bằng đúng quyền hiện có của app (process "sh" thường, KHÔNG tự xin root
     * qua "su") trên 1 thread nền, không chặn main thread và không quan tâm kết quả trả về.
     */
    private fun runScript(script: String) {
        Thread {
            try {
                val process = ShellExecutor.getRuntime()
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write(script)
                    writer.write("\nexit\n")
                    writer.flush()
                }
                process.waitFor()
            } catch (e: Exception) {
            }
        }.apply { isDaemon = true }.start()
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