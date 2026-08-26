package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import com.tool.tree.R

/**
 * Dialog toàn màn hình có hỗ trợ vuốt sang phải để đóng (kèm blur background trượt theo)
 * và predictive-back (vuốt từ mép) trên Android 13+.
 *
 * Sử dụng:
 * class MyDialog(darkMode: Boolean) : DialogFullScreen(R.layout.my_dialog, darkMode) {
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         // setup views
 *     }
 * }
 */
open class DialogFullScreen(
    private val layout: Int,
    darkMode: Boolean
) : DialogFragment() {

    // View gốc chứa toàn bộ nội dung + background blur
    private var rootView: FrameLayout? = null
    private var swipeBackHelper: DialogSwipeBackHelper? = null
    private var backCallback: OnBackInvokedCallback? = null

    // Cho phép tắt vuốt đóng nếu dialog có cử chỉ kéo riêng (HorizontalScrollView,...)
    protected var swipeToDismissEnabled = true

    // Theme
    private val themeResId = if (darkMode) R.style.dialog_full_screen_dark else R.style.dialog_full_screen_light

    // ================== Lifecycle ==================

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Dialog(requireActivity(), themeResId)
        } else {
            Dialog(requireActivity(), -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = context ?: return null

        // Tạo container chính chiếm toàn màn hình
        rootView = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Đặt background blur (chụp từ Activity phía sau)
            background = createBlurBackground()

            // Inflate layout của người dùng và thêm vào container
            val content = inflater.inflate(layout, this, false)
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = dialog ?: return
        val root = rootView ?: return

        // Gắn helper vuốt lùi (tác động lên rootView)
        if (swipeToDismissEnabled) {
            swipeBackHelper = DialogSwipeBackHelper.bind(
                dialog,
                root,
                onDragStateChanged = { /* tuỳ chọn */ },
                onDragProgress = { /* tuỳ chọn */ },
                onBack = ::closeView
            )
        }

        // Đăng ký predictive-back (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val helper = swipeBackHelper
            if (helper != null) {
                val callback = object : OnBackInvokedCallback {
                    override fun onBackStarted() {
                        helper.onSystemBackStarted()
                    }

                    override fun onBackProgress(backEvent: BackEvent) {
                        helper.onSystemBackProgress(backEvent.progress)
                    }

                    override fun onBackCancelled() {
                        helper.onSystemBackCancelled()
                    }

                    override fun onBackInvoked() {
                        // Nếu helper đã xử lý (đang có cử chỉ dở dang) thì không dismiss thêm
                        if (!helper.consumeSystemBackInvoked()) {
                            closeView()
                        }
                    }
                }
                dialog.window?.onBackInvokedDispatcher?.registerOnBackInvokedCallback(
                    OnBackInvokedCallback.PRIORITY_DEFAULT,
                    callback
                )
                backCallback = callback
            }
        }
    }

    override fun onDestroyView() {
        // Hủy đăng ký predictive-back
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback?.let {
                dialog?.window?.onBackInvokedDispatcher?.unregisterOnBackInvokedCallback(it)
            }
            backCallback = null
        }

        // Giải phóng helper
        swipeBackHelper?.release()
        swipeBackHelper = null
        rootView = null

        super.onDestroyView()
    }

    // ================== Hàm đóng dialog ==================

    fun closeView() {
        try {
            dismiss()
        } catch (_: Exception) { /* ignore */ }
    }

    // ================== Tạo ảnh nền blur ==================

    /**
     * Lấy ảnh blur từ Activity phía sau, dùng chung [FastBlurUtility]
     * giống như trong [DialogHelper].
     */
    private fun createBlurBackground(): android.graphics.drawable.Drawable? {
        val activity = activity ?: return null
        return FastBlurUtility.getBlurBackgroundDrawer(activity)
    }
}