package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.core.graphics.drawable.toDrawable
import com.tool.tree.R

open class DialogFullScreen(private val layout: Int, darkMode: Boolean) : androidx.fragment.app.DialogFragment() {

    private var themeResId: Int = 0
    private lateinit var currentView: View

    protected var swipeToDismissEnabled = true
    private var swipeBackHelper: DialogSwipeBackHelper? = null

    // Callback cho Predictive Back trên Android 13+ (API 33+)
    private var systemBackCallback: OnBackInvokedCallback? = null

    init {
        themeResId = if (darkMode) R.style.dialog_full_screen_dark else R.style.dialog_full_screen_light
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dialog(requireActivity(), if (themeResId != 0) themeResId else R.style.dialog_full_screen_light)
        } else {
            Dialog(requireActivity(), -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentView = inflater.inflate(layout, container, false)
        return currentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = this.activity ?: return

        dialog?.window?.run {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setWindowAnimations(android.R.style.Animation_Translucent)
            }
            // Gọi đúng hàm nhận Window của DialogHelper
            DialogHelper.setWindowBlurBg(this, activity)
        }

        // Khởi tạo SwipeBackHelper & đăng ký Predictive Back
        if (swipeToDismissEnabled) {
            dialog?.let { dlg ->
                swipeBackHelper = DialogSwipeBackHelper.bind(
                    dialog = dlg,
                    contentView = view,
                    onDragStateChanged = { dragging ->
                        // Khi bắt đầu kéo, ẩn nền window blur đi để lộ activity phía sau trượt theo
                        val window = dlg.window
                        if (dragging) {
                            window?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
                        } else {
                            window?.let { DialogHelper.setWindowBlurBg(it, activity) }
                        }
                    },
                    onDragProgress = { /* Xử lý thêm nếu cần */ }
                ) {
                    closeView()
                }

                // Tích hợp vuốt mép hệ thống (Predictive Back - Android 13+)
                registerSystemPredictiveBack(dlg)
            }
        }
    }

    /**
     * Đăng ký Predictive Back Dispatcher với Window của Dialog (Android 13+)
     */
    private fun registerSystemPredictiveBack(dialog: Dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val window = dialog.window ?: return

            // API 34+ hỗ trợ Animation progress khi vuốt mép
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                systemBackCallback = object : android.window.OnBackAnimationCallback {
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
            } else {
                // API 33 fallback
                systemBackCallback = OnBackInvokedCallback {
                    if (swipeBackHelper?.consumeSystemBackInvoked() != true) {
                        closeView()
                    }
                }
            }

            systemBackCallback?.let { callback ->
                window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    callback
                )
            }
        }
    }

    /**
     * Hủy đăng ký Predictive Back Dispatcher
     */
    private fun unregisterSystemPredictiveBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val window = dialog?.window ?: return
            systemBackCallback?.let { callback ->
                window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
            systemBackCallback = null
        }
    }

    override fun onDestroyView() {
        unregisterSystemPredictiveBack()
        swipeBackHelper?.release()
        swipeBackHelper = null
        super.onDestroyView()
    }

    fun closeView() {
        try {
            dismiss()
        } catch (ex: Exception) {
            // Ignore
        }
    }
}
