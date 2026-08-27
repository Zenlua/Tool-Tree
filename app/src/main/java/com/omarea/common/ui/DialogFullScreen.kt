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

open class DialogFullScreen(private val layout: Int, private val darkMode: Boolean) : androidx.fragment.app.DialogFragment() {
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
        val d = dialog
        if (activity != null && d != null) {
            d.window?.run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    setWindowAnimations(android.R.style.Animation_Translucent)
                }
                DialogHelper.applyEdgeToEdge(this, darkMode, view)
            }

            if (swipeToDismissEnabled) {
                // QUAN TRỌNG: view.parent vẫn còn null tại đây - AndroidX DialogFragment chỉ
                // thật sự gọi dialog.setContentView(view) ở onActivityCreated() (chạy SAU
                // onViewCreated()), nên DialogSwipeBackBlurWrapper.wrap() gọi ngay tại chỗ này
                // sẽ luôn thấy parent null và fallback về nền blur tĩnh cũ. view.post() đợi 1
                // vòng của UI thread - lúc đó setContentView() đã chạy xong, view đã có parent.
                view.post {
                    val window = d.window ?: return@post
                    val swipeTarget = DialogSwipeBackBlurWrapper.wrap(activity, window, view) ?: run {
                        DialogHelper.setWindowBlurBg(window, activity)
                        view
                    }
                    swipeBackHelper = DialogSwipeBackHelper.bind(d, swipeTarget) { closeView() }
                    // Vuốt từ mép màn hình (predictive-back hệ thống, API 33+) - dùng chung
                    // tiến độ với vuốt tay trực tiếp ở trên (xem DialogPredictiveBackBinder).
                    predictiveBackCallback = swipeBackHelper?.let { DialogPredictiveBackBinder.bind(d, it) }
                }
            } else {
                d.window?.run { DialogHelper.setWindowBlurBg(this, activity) }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        dialog?.let { DialogPredictiveBackBinder.unbind(it, predictiveBackCallback) }
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