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

                if (swipeToDismissEnabled) {
                    // Bọc view + ảnh blur chung 1 wrapper để cả khối trượt cùng nhau khi kéo
                    // (xem DialogSwipeBackBlurWrapper). Không bọc được thì fallback về hành vi
                    // cũ: nền blur cố định + chỉ view trượt, không có gì lộ ra khi kéo.
                    val swipeTarget = DialogSwipeBackBlurWrapper.wrap(activity, this, view) ?: run {
                        DialogHelper.setWindowBlurBg(this, activity)
                        view
                    }
                    swipeBackHelper = DialogSwipeBackHelper.bind(d, swipeTarget) { closeView() }
                } else {
                    DialogHelper.setWindowBlurBg(this, activity)
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
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
