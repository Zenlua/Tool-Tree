package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
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

    // Nền blur/màu phẳng ban đầu của window (set bởi DialogHelper.setWindowBlurBg ở
    // onViewCreated) - lưu lại để trả về CHÍNH XÁC như cũ (không tính lại blur) khi 1 lượt vuốt
    // bị hủy giữa chừng, xem onSwipeDragStateChanged() bên dưới.
    private var originalWindowBackground: android.graphics.drawable.Drawable? = null

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

                DialogHelper.setWindowBlurBg(this, activity)
                // Chụp lại đúng Drawable vừa set (blur hoặc màu phẳng) để có gì trả về y hệt lúc
                // hủy vuốt - không gọi lại setWindowBlurBg() (tránh tính blur lại tốn kém).
                originalWindowBackground = decorView.background
            }

            if (swipeToDismissEnabled) {
                dialog?.let {
                    swipeBackHelper = DialogSwipeBackHelper.bind(
                        it, view,
                        onDragStateChanged = { dragging -> onSwipeDragStateChanged(dragging) }
                    ) { closeView() }
                }
            }
        }
    }

    // Toàn bộ nội dung dialog (contentView) KHÔNG đổi gì khác ngoài translationX (xem
    // DialogSwipeBackHelper.applyProgress) - ở đây chỉ lo phần NỀN của window: vừa nhận diện là
    // đang kéo -> bỏ ngay nền blur/màu phẳng, chuyển trong suốt HOÀN TOÀN NGAY LẬP TỨC (không
    // crossfade dần) để lộ đúng Activity thật đang sống phía sau đúng theo tay kéo, giống như
    // đang kéo 1 tấm cửa sổ vật lý sang bên phải. Nếu vuốt bị hủy (bật lại vị trí cũ) thì trả
    // lại NGUYÊN VẸN Drawable nền ban đầu đã lưu ở onViewCreated().
    private fun onSwipeDragStateChanged(dragging: Boolean) {
        val window = dialog?.window ?: return
        if (dragging) {
            window.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        } else {
            originalWindowBackground?.let { window.setBackgroundDrawable(it) }
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