package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.core.graphics.drawable.toDrawable
import com.tool.tree.R

open class DialogFullScreen(private val layout: Int, darkMode: Boolean) : androidx.fragment.app.DialogFragment() {

    private var themeResId: Int = 0
    private lateinit var currentView: View

    protected var swipeToDismissEnabled = true
    private var swipeBackHelper: DialogSwipeBackHelper? = null

    // Callback cử chỉ Predictive Back hệ thống (Android 13+)
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
            // 1. Giữ Window luôn trong suốt để lộ Activity thật phía dưới khi currentView trượt đi
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        }

        // 2. Chụp và gán Blur trực tiếp làm NỀN CỦA currentView
        currentView.post {
            setupViewBlurBackground(activity)
        }

        // 3. Đăng ký cử chỉ vuốt trượt cửa sổ & Vuốt mép (Predictive Back)
        if (swipeToDismissEnabled) {
            dialog?.let { dlg ->
                swipeBackHelper = DialogSwipeBackHelper.bind(
                    dialog = dlg,
                    contentView = view,
                    onDragStateChanged = { /* Nền window đã trong suốt sẵn, không cần can thiệp */ },
                    onDragProgress = { /* Cập nhật hiệu ứng phụ nếu cần */ }
                ) {
                    closeView()
                }

                registerSystemPredictiveBack(dlg)
            }
        }
    }

    /**
     * Lấy Blur từ FastBlurUtility và set trực tiếp vào View nội dung
     * giúp lớp Blur trượt đồng bộ theo translationX của currentView.
     */
    private fun setupViewBlurBackground(activity: android.app.Activity) {
        if (!DialogHelper.disableBlurBg) {
            val blurBitmap = FastBlurUtility.getBlurBackgroundDrawer(activity)
            if (blurBitmap != null) {
                currentView.background = blurBitmap.toDrawable(activity.resources)
                return
            }
        }
        // Fallback màu nền phẳng nếu không tạo được blur
        val isDark = ThemeModeState.isDarkMode()
        val defaultBgColor = if (isDark) {
            android.graphics.Color.argb(255, 18, 18, 18)
        } else {
            android.graphics.Color.argb(255, 245, 245, 245)
        }
        currentView.setBackgroundColor(defaultBgColor)
    }

    /**
     * Đăng ký Predictive Back (vuốt từ mép màn hình) với Window của Dialog (Android 13+)
     */
    private fun registerSystemPredictiveBack(dialog: Dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val window = dialog.window ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ (API 34+): Hỗ trợ animation vuốt mép có độ trễ/tiến độ (progress)
                systemBackCallback = object : OnBackAnimationCallback {
                    override fun onBackStarted(backEvent: BackEvent) {
                        swipeBackHelper?.onSystemBackStarted()
                    }

                    override fun onBackProgressed(backEvent: BackEvent) {
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
                // Android 13 (API 33): Fallback phản hồi sự kiện back đơn thuần
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
        } catch (_: Exception) {
        }
    }
}
