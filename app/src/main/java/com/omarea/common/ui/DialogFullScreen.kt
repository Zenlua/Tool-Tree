package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tool.tree.R


/*
继承使用示例：

class DialogAppChooser(private val darkMode: Boolean): DialogFullScreen(R.layout.dialog_app_chooser, darkMode) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }
}
*/

open class DialogFullScreen(private val layout: Int, darkMode: Boolean) : androidx.fragment.app.DialogFragment() {
    // currentView = nội dung THẬT của dialog (kết quả inflate(layout)) - đây là view được
    // trượt đi lúc vuốt để đóng. Không còn cần 1 lớp "revealView" ảnh chụp riêng đứng yên bên
    // dưới nữa - nền CỦA CHÍNH CỬA SỔ dialog giờ đã được set thẳng bằng ảnh nét (xem
    // onViewCreated() -> DialogHelper.setWindowBlurBgWithSharpCopy()), nên khi currentView
    // trượt sang phải lúc vuốt, nó tự lộ ra đúng ảnh nét đó phía sau - không cần thêm view nào.
    private lateinit var currentView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflated = inflater.inflate(layout, container, false)
        currentView = inflated
        return inflated
    }

    private var themeResId: Int = 0

    // Cho vuốt sang phải để đóng dialog (xem onViewCreated() bên dưới / DialogSwipeBackHelper).
    // Dialog con nào có cử chỉ kéo ngang riêng cần ưu tiên hơn (hiếm) có thể gán false TRƯỚC
    // khi view được dựng (super.onViewCreated()) để tắt tính năng này.
    protected var swipeToDismissEnabled = true
    private var swipeBackHelper: DialogSwipeBackHelper? = null
    private var predictiveBackCallback: Any? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dialog(activity!!, if (themeResId != 0) themeResId else R.style.dialog_full_screen_light)
        } else {
            Dialog(activity!!, -1)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity
        if (activity != null) {
            dialog?.window?.run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    setWindowAnimations(android.R.style.Animation_Translucent)
                }

                // Set thẳng nền cửa sổ dialog = ảnh NÉT màn hình phía sau (không làm mờ) - xem
                // giải thích ở DialogHelper.setWindowBlurBgWithSharpCopy(). Không cần giữ lại
                // bitmap trả về nữa vì không còn revealView nào phải gán ảnh cho.
                DialogHelper.setWindowBlurBgWithSharpCopy(this, activity)
            }

            if (swipeToDismissEnabled) {
                dialog?.let { d ->
                    swipeBackHelper = DialogSwipeBackHelper.bind(
                        dialog = d,
                        contentView = currentView,
                        onBack = { closeView() }
                    )
                    setupPredictiveBack(d)
                }
            }
        }
    }

    /**
     * Đăng ký vuốt-từ-mép (predictive-back của hệ thống, Android 13+) cho ĐÚNG Window của
     * Dialog này - Dialog có Window riêng, không tự động nhận được cử chỉ qua
     * activity.onBackPressedDispatcher như 1 trang toàn màn hình thường (xem ActionPage.kt).
     * Trên Android 14+ (UPSIDE_DOWN_CAKE) dùng OnBackAnimationCallback để có cả tiến độ kéo mượt
     * (giống hệt hành vi ở SwipeBackHelper); trên Android 13 chỉ có OnBackInvokedCallback (không
     * có tiến độ), coi như buông tay là đóng luôn - vẫn còn hơn không hỗ trợ gì cả.
     */
    private fun setupPredictiveBack(dialog: Dialog) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val window = dialog.window ?: return
        val helper = swipeBackHelper ?: return
        val dispatcher = window.onBackInvokedDispatcher

        val callback: android.window.OnBackInvokedCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                object : android.window.OnBackAnimationCallback {
                    override fun onBackStarted(backEvent: android.window.BackEvent) {
                        helper.onSystemBackStarted()
                    }

                    override fun onBackProgressed(backEvent: android.window.BackEvent) {
                        helper.onSystemBackProgress(backEvent.progress)
                    }

                    override fun onBackCancelled() {
                        helper.onSystemBackCancelled()
                    }

                    override fun onBackInvoked() {
                        if (!helper.consumeSystemBackInvoked()) {
                            closeView()
                        }
                    }
                }
            } else {
                android.window.OnBackInvokedCallback {
                    if (!helper.consumeSystemBackInvoked()) {
                        closeView()
                    }
                }
            }

        try {
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            predictiveBackCallback = callback
        } catch (_: Exception) {
            // Hiếm khi Window chưa sẵn sàng nhận đăng ký - bỏ qua, dialog vẫn đóng bình thường
            // bằng vuốt tay trực tiếp (DialogSwipeBackHelper.dispatchTouchEvent) hoặc nút back
            // thường (không có animation kéo theo).
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (predictiveBackCallback as? android.window.OnBackInvokedCallback)?.let {
                try {
                    dialog?.window?.onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(it)
                } catch (_: Exception) {
                }
            }
        }
        predictiveBackCallback = null
        swipeBackHelper?.release()
        swipeBackHelper = null
        super.onDestroyView()
    }

    fun closeView() {
        try {
            dismiss()
        } catch (ex: java.lang.Exception) {
        }
    }

    init {
        themeResId = if (darkMode) R.style.dialog_full_screen_dark else R.style.dialog_full_screen_light
    }
}
