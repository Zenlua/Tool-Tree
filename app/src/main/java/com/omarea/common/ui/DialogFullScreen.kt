package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tool.tree.R

open class DialogFullScreen(private val layout: Int, darkMode: Boolean) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val theme = if (themeResId != 0) themeResId else R.style.dialog_full_screen_light
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dialog(requireContext(), theme)
        } else {
            Dialog(requireContext(), -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout, container, false)
    }

    protected var swipeToDismissEnabled = true
    private var swipeBackHelper: DialogSwipeBackHelper? = null
    
    // Dùng Any? để tránh lỗi lint ở các dòng dưới đối với device < Android 13
    private var backInvokedCallback: Any? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        dialog?.window?.run {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                setWindowAnimations(android.R.style.Animation_Translucent)
            }
            DialogHelper.setWindowBlurBg(this, activity)
        }

        if (swipeToDismissEnabled) {
            dialog?.let { d ->
                // Truyền DecorView để trượt cả hiệu ứng blur/nền của dialog
                val targetView = d.window?.decorView ?: view
                swipeBackHelper = DialogSwipeBackHelper.bind(d, targetView) { closeView() }
                
                // Đăng ký Predictive Back (Vuốt từ mép) cho Android 13+
                setupPredictiveBack(d)
            }
        }
    }

    /**
     * Đăng ký OnBackInvokedCallback trực tiếp vào Window của Dialog.
     * (Dialog không dùng chung onBackPressedDispatcher của Activity được)
     */
    private fun setupPredictiveBack(dialog: Dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val dispatcher = dialog.window?.onBackInvokedDispatcher ?: return
            val helper = swipeBackHelper ?: return

            // Tạo callback cho Predictive Back
            val callback = object : android.window.OnBackInvokedCallback {
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
                    // Nếu helper đang xử lý cử chỉ vuốt, nó sẽ trả về true và tự gọi onBack()
                    // Nếu không (trường hợp bấm nút back vật lý chẳng hạn), closeView() sẽ được gọi
                    if (!helper.consumeSystemBackInvoked()) {
                        closeView()
                    }
                }
            }

            // Ưu tiên 0 (mặc định). Đăng ký vào Window dispatcher
            dispatcher.registerOnBackInvokedCallback(0, callback)
            backInvokedCallback = callback
        }
    }

    override fun onDestroyView() {
        // Hủy đăng ký Predictive Back để tránh leak memory
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            dialog?.window?.onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(
                backInvokedCallback as? android.window.OnBackInvokedCallback
            )
            backInvokedCallback = null
        }

        swipeBackHelper?.release()
        swipeBackHelper = null
        super.onDestroyView()
    }

    fun closeView() {
        try {
            dismiss()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        private var themeResId: Int = 0
    }

    init {
        themeResId = if (darkMode) R.style.dialog_full_screen_dark else R.style.dialog_full_screen_light
    }
}