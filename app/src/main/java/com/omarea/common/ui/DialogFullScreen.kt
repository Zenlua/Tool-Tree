package com.omarea.common.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(layout, container)
        return currentView
    }

    private var themeResId: Int = 0
    private lateinit var currentView: View

    // Cho vuốt sang phải để đóng dialog (xem onViewCreated() bên dưới / DialogSwipeBackHelper).
    // Dialog con nào có cử chỉ kéo ngang riêng cần ưu tiên hơn (hiếm) có thể gán false TRƯỚC
    // khi view được dựng (super.onViewCreated()) để tắt tính năng này.
    protected var swipeToDismissEnabled = true
    private var swipeBackHelper: DialogSwipeBackHelper? = null
    private var backInvokedCallback: Any? = null

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
            val window = dialog?.window
            window?.run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    setWindowAnimations(android.R.style.Animation_Translucent)
                }

                // Tạo blur background từ DialogHelper
                DialogHelper.setWindowBlurBg(this, activity)

                // Đưa background blur từ Window sang root view (contentView) để khi vuốt,
                // lớp blur sẽ đi theo cùng màn hình thay vì đứng yên ở nền cửa sổ window.
                val blurBg = decorView.background
                if (blurBg != null) {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    view.background = blurBg
                }
            }

            if (swipeToDismissEnabled) {
                dialog?.let { dlg ->
                    swipeBackHelper = DialogSwipeBackHelper.bind(dlg, view) { closeView() }
                }
            }

            // Tích hợp cử chỉ vuốt từ mép (Predictive Back) cho Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                window?.onBackInvokedDispatcher?.let { dispatcher ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        // Android 14+ hỗ trợ callback có animation tiến độ đầy đủ
                        val callback = object : android.window.OnBackAnimationCallback {
                            override fun onBackStarted(backEvent: android.window.BackEvent) {
                                swipeBackHelper?.onSystemBackStarted()
                            }
                            override fun onBackProgressed(backEvent: android.window.BackEvent) {
                                swipeBackHelper?.onSystemBackProgress(backEvent.progress)
                            }
                            override fun onBackCancelled() {
                                swipeBackHelper?.onSystemBackCancelled()
                            }
                            override fun onBackInvoked() {
                                if (swipeBackHelper?.consumeSystemBackInvoked() != true) {
                                    closeView()
                                }
                            }
                        }
                        dispatcher.registerOnBackInvokedCallback(0, callback)
                        backInvokedCallback = callback
                    } else {
                        // Android 13 (API 33) hỗ trợ callback cơ bản
                        val callback = android.window.OnBackInvokedCallback {
                            if (swipeBackHelper?.consumeSystemBackInvoked() != true) {
                                closeView()
                            }
                        }
                        dispatcher.registerOnBackInvokedCallback(0, callback)
                        backInvokedCallback = callback
                    }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        // Hủy đăng ký Predictive Back callback để tránh leak memory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { callback ->
                val dispatcher = dialog?.window?.onBackInvokedDispatcher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    dispatcher?.unregisterOnBackInvokedCallback(callback as android.window.OnBackAnimationCallback)
                } else {
                    dispatcher?.unregisterOnBackInvokedCallback(callback as android.window.OnBackInvokedCallback)
                }
            }
            backInvokedCallback = null
        }

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
