package com.omarea.common.ui

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tool.tree.R
import androidx.core.graphics.drawable.toDrawable
import com.tool.tree.ThemeModeState
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

class DialogHelper {
    class DialogButton(val text: String, val onClick: Runnable? = null, val dismiss: Boolean = true)

    class DialogWrap(private val d: AlertDialog) {
        val context: Context = dialog.context
        private var mCancelable = true
        val isCancelable: Boolean
            get () {
                return mCancelable
            }

        fun setCancelable(cancelable: Boolean): DialogWrap {
            mCancelable = cancelable
            d.setCancelable(cancelable)

            return this
        }

        fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener): DialogWrap {
            d.setOnDismissListener(onDismissListener)

            return this
        }

        val dialog: AlertDialog
            get() {
                return d
            }

        fun dismiss() {
            try {
                d.dismiss()
            } catch (_: Exception) {
            }
        }

        fun hide() {
            try {
                d.hide()
            } catch (_: Exception) {
            }
        }

        val isShowing: Boolean
            get() {
                return d.isShowing
            }
    }

    companion object {
        // 是否禁用模糊背景
        var disableBlurBg = false

        fun animDialog(dialog: AlertDialog?): DialogWrap? {
            if (dialog != null && !dialog.isShowing) {
                dialog.window?.run {
                    setWindowAnimations(R.style.windowAnim)
                }
                dialog.show()
            }
            return if (dialog != null) DialogWrap(dialog) else null
        }

        fun animDialog(builder: AlertDialog.Builder): DialogWrap {
            val dialog = builder.create()
            animDialog(dialog)
            return DialogWrap(dialog)
        }

        fun helpInfo(context: Context, message: String, onDismiss: Runnable? = null): DialogWrap {
            return helpInfo(context, context.getString(R.string.help_title), message, onDismiss)
        }

        fun helpInfo(context: Context, title: String, message: String, onDismiss: Runnable? = null): DialogWrap {
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_help_info, null)

            (dialog.findViewById<TextView>(R.id.confirm_title)!!).run {
                if (title.isNotEmpty()) {
                    text = title
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            (dialog.findViewById<TextView>(R.id.confirm_message)!!).run {
                if (message.isNotEmpty()) {
                    text = message
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            val d = customDialog(context, dialog, onDismiss == null)
            (dialog.findViewById<View>(R.id.btn_confirm)!!).run {
                if (onDismiss != null) {
                    d.setOnDismissListener {
                        onDismiss.run()
                    }
                }
                setOnClickListener {
                    d.dismiss()
                }
            }

            return d
        }

        fun helpInfo(context: Context,
                  title: String = "",
                  message: String = "",
                  contentView: View,
                  onConfirm: Runnable? = null): DialogWrap {
            val view = getCustomDialogView(context, R.layout.dialog_help_info, title, message, contentView)

            val dialog = customDialog(context, view)
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null): DialogWrap {
            return openContinueAlert(context, R.layout.dialog_confirm, title, message, onConfirm, onCancel)
        }

        fun warning(context: Context,
                    title: String = "",
                    message: String = "",
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null,
                    cancelable: Boolean = true): DialogWrap {
            return openContinueAlert(context, R.layout.dialog_warning, title, message, onConfirm, onCancel, cancelable)
        }

        private fun getCustomDialogView(context: Context,
                                        layout: Int,
                                        title: String = "",
                                        message: String = "",
                                        contentView: View? = null): View {

            val view = LayoutInflater.from(context).inflate(layout, null)
            view.findViewById<TextView?>(R.id.confirm_title)?.run {
                if (title.isEmpty()) {
                    visibility = View.GONE
                } else {
                    text = title
                }
            }

            view.findViewById<TextView?>(R.id.confirm_message)?.run {
                if (message.isEmpty()) {
                    visibility = View.GONE
                } else {
                    text = message
                }
            }

            if (contentView != null) {
                view.findViewById<FrameLayout?>(R.id.confirm_custom_view)?.addView(contentView)
            }

            return view
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    contentView: View? = null,
                    onConfirm: Runnable? = null,
                    onCancel: Runnable? = null): DialogWrap {
            val view = getCustomDialogView(context, R.layout.dialog_confirm, title, message, contentView)

            val dialog = customDialog(context, view)
            view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                dialog.dismiss()
                onCancel?.run()
            }
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    onConfirm: DialogButton? = null,
                    onCancel: DialogButton? = null): DialogWrap {
            return confirm(context, title, message, null, onConfirm, onCancel)
        }

        fun confirm(context: Context,
                    title: String = "",
                    message: String = "",
                    contentView: View? = null,
                    onConfirm: DialogButton? = null,
                    onCancel: DialogButton? = null): DialogWrap {
            val view = getCustomDialogView(context, R.layout.dialog_confirm, title, message, contentView)

            val dialog = customDialog(context, view)

            val btnConfirm = view.findViewById<TextView?>(R.id.btn_confirm)
            if (onConfirm != null) {
                btnConfirm?.text = onConfirm.text
            }
            btnConfirm?.setOnClickListener {
                if (onConfirm != null) {
                    if (onConfirm.dismiss) {
                        dialog.dismiss()
                    }
                    onConfirm.onClick?.run()
                } else {
                    dialog.dismiss()
                }
            }


            val btnCancel = view.findViewById<TextView?>(R.id.btn_cancel)
            if (onCancel != null) {
                btnCancel?.text = onCancel.text
            }
            btnCancel?.setOnClickListener {
                if (onCancel != null) {
                    if (onCancel.dismiss) {
                        dialog.dismiss()
                    }
                    onCancel.onClick?.run()
                } else {
                    dialog.dismiss()
                }
            }

            return dialog
        }

        fun confirm(context: Context, contentView: View? = null, onConfirm: DialogButton? = null, onCancel: DialogButton? = null): DialogWrap {
            return this.confirm(context, "", "", contentView, onConfirm, onCancel)
        }

        private fun getWindowBackground(context: Context, defaultColor: Int = Color.TRANSPARENT): Int {
            val attrsArray = intArrayOf(android.R.attr.background)
            val typedArray = context.obtainStyledAttributes(attrsArray)
            val color = typedArray.getColor(0, defaultColor)
            typedArray.recycle()
            return color
        }

        private fun setOutsideTouchDismiss(view: View, dialogWrap: DialogWrap): DialogWrap {
            val dialog = dialogWrap.dialog
            val rootView = dialog.window?.decorView
            rootView?.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event != null && event.action == MotionEvent.ACTION_UP) {
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        val rect = Rect()
                        view.getGlobalVisibleRect(rect)
                        if (!rect.contains(x, y)) {
                            val mCancelable = dialogWrap.isCancelable
                            if (mCancelable) {
                                dialogWrap.dismiss()
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            return dialogWrap
        }

        private fun getStatusBarColor(context: Context): Int {
            val defaultColor = Color.WHITE
            val attrsArray = intArrayOf(android.R.attr.statusBarColor)
            val typedArray = context.obtainStyledAttributes(attrsArray)
            val color = typedArray.getColor(0, defaultColor)
            typedArray.recycle()
            return color
        }

        private fun openContinueAlert(context: Context,
                                      layout: Int,
                                      title: String = "",
                                      message: String = "",
                                      onConfirm: Runnable? = null,
                                      onCancel: Runnable? = null,
                                      cancelable: Boolean = true): DialogWrap {
            val view = getCustomDialogView(context, layout, title, message, null)

            val dialog = customDialog(context, view, cancelable)
            view.findViewById<View?>(R.id.btn_cancel)?.setOnClickListener {
                dialog.dismiss()
                onCancel?.run()
            }
            view.findViewById<View?>(R.id.btn_confirm)?.setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun confirmBlur(context: Activity,
                        title: String = "",
                        message: String = "",
                        onConfirm: Runnable? = null,
                        onCancel: Runnable? = null): DialogWrap {
            return openContinueAlert(context, R.layout.dialog_confirm, title, message, onConfirm, onCancel)
        }

        fun alert(context: Context,
                  title: String = "",
                  message: String = "",
                  onConfirm: Runnable? = null): DialogWrap {
            return openContinueAlert(context, R.layout.dialog_alert, title, message, onConfirm, null)
        }

        fun alert(context: Context,
                  title: String = "",
                  message: String = "",
                  contentView: View,
                  onConfirm: Runnable? = null): DialogWrap {
            val view = getCustomDialogView(context, R.layout.dialog_alert, title, message, contentView)

            val dialog = customDialog(context, view)
            view.findViewById<View>(R.id.btn_confirm).setOnClickListener {
                dialog.dismiss()
                onConfirm?.run()
            }

            return dialog
        }

        fun customDialog(context: Context, view: View, cancelable: Boolean = true): DialogWrap {
            val useBlur = (
                        context is Activity &&
                        context.window.attributes.flags and WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER == 0
                    )

            val dialog = (if (useBlur) {
                AlertDialog.Builder(context, R.style.custom_alert_dialog)
            } else {
                AlertDialog.Builder(context)
            }).setView(view).setCancelable(cancelable).create()

            if (context is Activity) {
                // Tính & set nền blur TRƯỚC khi show() để không có khung hình nào
                // dialog hiện ra mà chưa có nền mờ (tránh nháy mờ/rõ). Nếu cancelable, ngay sau
                // show() sẽ thử "nâng cấp" lên bản blur trượt cùng nội dung + bind vuốt lùi (xem
                // dưới) - bản tĩnh này chỉ còn là fallback lúc đó.
                dialog.window?.run {
                    setWindowBlurBg(this, context)
                    decorView.run {
                        systemUiVisibility = context.window.decorView.systemUiVisibility // View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                    if (useBlur) {
                        // Mặc định (decorFitsSystemWindows=true) window CHỈ trải nội dung ra tới
                        // mép status/navigation bar - android.R.id.content bị hệ thống tự cộng
                        // padding = kích thước 2 thanh đó. Ảnh blur (match_parent bên trong
                        // wrapper, xem DialogSwipeBackBlurWrapper.wrap()) vì vậy cũng bị thu hẹp
                        // theo, để lộ 2 dải KHÔNG blur ở trên/dưới màn hình. Gọi
                        // applyEdgeToEdge() (contentView=null - KHÔNG cộng padding bù insets cho
                        // dialogView nhỏ, nó vốn đã canh giữa, không cần né status/nav bar) để
                        // window vẽ tràn viền thật, cho ảnh blur phủ kín toàn màn hình.
                        applyEdgeToEdge(this, isNightMode(context))
                    }
                }
                dialog.show()

                if (useBlur && cancelable) {
                    // Mọi dialog dùng nền blur (customDialog là nơi DUY NHẤT gọi setWindowBlurBg
                    // ở trên) đều được vuốt sang phải để đóng, dùng chung cơ chế với
                    // DialogFullScreen/kr_dialog_params (xem DialogFullScreen.bindSwipeToDismiss())
                    // - không cancelable thì giữ nguyên nền blur tĩnh, không có vuốt lùi. Chỉ áp
                    // dụng khi useBlur=true (theme custom_alert_dialog, window phủ toàn màn hình
                    // trong suốt) - nhánh useBlur=false dùng theme AlertDialog nổi mặc định
                    // (windowIsFloating=true), không hợp với hiệu ứng "trượt lộ nền phía sau".
                    val swipeBinding = DialogFullScreen.bindSwipeToDismiss(context, dialog) { dialog.dismiss() }
                    if (swipeBinding != null) {
                        dialog.setOnDismissListener { swipeBinding.release(dialog) }
                    }
                }
            } else {
                dialog.window?.run {
                    setWindowAnimations(R.style.windowAnim2)
                }
                dialog.show()
                dialog.window?.run {
                    setBackgroundDrawableResource(android.R.color.transparent)
                }
            }

            return setOutsideTouchDismiss(view, DialogWrap(dialog).setCancelable(cancelable))
        }

        private fun isNightMode(context: Context): Boolean {
            return ThemeModeState.isDarkMode()
        }

        /**
         * Dialog full-screen (DialogFullScreen, kr_dialog_params khi isLongList) có Window RIÊNG,
         * KHÔNG tự "thừa hưởng" cờ edge-to-edge mà ThemeModeState.applyWindowFlags() đã set cho
         * Window của Activity - nếu không tự gọi lại ở đây, status bar/navigation bar của dialog
         * sẽ không được vẽ xuyên qua (mất hiệu ứng mờ) dù các style dialog_full_screen (light/dark)
         * và kr_full_screen_dialog (light/dark) đã khai statusBarColor/navigationBarColor =
         * transparent (chỉ khai màu, không tự bật e-t-e thật sự).
         *
         * LƯU Ý: bật setDecorFitsSystemWindows(false) đồng nghĩa hệ thống KHÔNG còn tự chừa chỗ
         * cho status bar/navigation bar nữa - nội dung dialog sẽ tự vẽ tràn lên đè cả 2 thanh đó
         * nếu không tự pad lại. Truyền contentView (root view thật sự của dialog, ví dụ view của
         * DialogFullScreen hoặc dialogView của kr_dialog_params) để hàm này tự lắng nghe
         * WindowInsets và CỘNG THÊM đúng phần bị che (systemBars) vào padding GỐC đã khai sẵn ở
         * layout/style (vd dialogRoot padding=12dp) - không ghi đè mất padding cũ.
         */
        fun applyEdgeToEdge(window: Window, darkMode: Boolean, contentView: View? = null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            val controller = WindowInsetsControllerCompat(window, window.decorView)
            val useLightIcons = !darkMode
            controller.isAppearanceLightStatusBars = useLightIcons
            controller.isAppearanceLightNavigationBars = useLightIcons

            if (contentView != null) {
                val basePaddingLeft = contentView.paddingLeft
                val basePaddingTop = contentView.paddingTop
                val basePaddingRight = contentView.paddingRight
                val basePaddingBottom = contentView.paddingBottom

                ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
                    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    v.setPadding(
                        basePaddingLeft + systemBars.left,
                        basePaddingTop + systemBars.top,
                        basePaddingRight + systemBars.right,
                        basePaddingBottom + systemBars.bottom + ime.bottom
                    )
                    insets
                }
                ViewCompat.requestApplyInsets(contentView)
            }
        }

        // Trong setWindowBlurBg
        fun setWindowBlurBg(window: Window, activity: Activity) {
            val wallpaperMode = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER != 0
            window.run {
                val blurBitmap = if (disableBlurBg) {
                    null
                } else {
                    FastBlurUtility.getDialogBlurBackground(activity) ?: if (wallpaperMode) {
                        // Chụp screenshot cho dialog thất bại (getDialogBlurBackground trả null,
                        // ví dụ decorView chưa có width/height hoặc lỗi khi vẽ) - nhưng theme
                        // hiện tại là theme hình nền (wallpaperMode) nên vẫn còn ảnh wallpaper
                        // đã blur sẵn trong cache (BlurEngine.blurBitmap). Dùng tạm ảnh đó thay
                        // vì rơi thẳng xuống màu nền đặc bên dưới, giữ đúng cảm giác "nền hình
                        // ảnh mờ" của theme thay vì đổi hẳn sang màu phẳng.
                        FastBlurUtility.getPageBlurBackground(activity)
                    } else {
                        null
                    }
                }
                if (blurBitmap != null) {
                    setBackgroundDrawable(blurBitmap.toDrawable(activity.resources))
                } else {
                    try {
                        val bg = getWindowBackground(activity)
                        if (bg == Color.TRANSPARENT) {
                            if (isFloating) {
                                setBackgroundDrawable(bg.toDrawable())
                                setDimAmount(0.8f)
                                return
                            } else {
                                val d = if (wallpaperMode || isNightMode(context)) {
                                    Color.argb(255, 18, 18, 18).toDrawable()
                                } else {
                                    Color.argb(255, 245, 245, 245).toDrawable()
                                }
                                setBackgroundDrawable(d)
                            }
                        } else {
                            setBackgroundDrawable(bg.toDrawable())
                        }
                    } catch (_: Exception) {
                        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    }
                }
            }
        }
    }
}