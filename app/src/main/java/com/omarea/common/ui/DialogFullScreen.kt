package com.omarea.common.ui

import android.app.Dialog
import android.graphics.Canvas
import android.graphics.drawable.Drawable
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
    
    // Wrapper để di chuyển blur background cùng với content
    private var blurBackgroundWrapper: BlurBackgroundWrapper? = null

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
                
                // Gắn wrapper để di chuyển blur khi kéo
                blurBackgroundWrapper = BlurBackgroundWrapper(this, view)
            }

            if (swipeToDismissEnabled) {
                dialog?.let { 
                    swipeBackHelper = DialogSwipeBackHelper.bind(
                        it, 
                        view,
                        onDragStateChanged = { dragging ->
                            // Xử lý thay đổi trạng thái kéo nếu cần
                        },
                        onDragProgress = { progress ->
                            // Di chuyển blur cùng với translationX của content
                            blurBackgroundWrapper?.setTranslationX(view.translationX)
                        },
                        onBack = { closeView() }
                    ) 
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        blurBackgroundWrapper?.release()
        blurBackgroundWrapper = null
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

    /**
     * Wrapper để di chuyển blur background drawable khi content view di chuyển
     */
    private class BlurBackgroundWrapper(private val window: android.view.Window, private val contentView: View) {
        private val originalDrawable: Drawable?
        private var wrappedDrawable: TranslatingDrawable? = null
        private var translationX = 0f

        init {
            // Lấy drawable hiện tại từ decorView
            originalDrawable = window.decorView.background
            if (originalDrawable != null) {
                wrappedDrawable = TranslatingDrawable(originalDrawable)
                window.decorView.background = wrappedDrawable
            }
        }

        fun setTranslationX(tx: Float) {
            translationX = tx
            wrappedDrawable?.setTranslationX(tx)
            // Trigger redraw
            contentView.invalidate()
        }

        fun release() {
            // Reset drawable về bình thường
            if (originalDrawable != null) {
                window.decorView.background = originalDrawable
            }
            wrappedDrawable = null
        }

        /**
         * Custom drawable hỗ trợ di chuyển màn hình khi vẽ
         */
        private class TranslatingDrawable(private val wrapped: Drawable) : Drawable() {
            private var translationX = 0f

            fun setTranslationX(tx: Float) {
                translationX = tx
            }

            override fun draw(canvas: Canvas) {
                // Lưu trạng thái canvas
                canvas.save()
                // Di chuyển canvas theo translationX của content view
                canvas.translate(translationX, 0f)
                // Vẽ drawable gốc tại vị trí đã tịnh tiến
                wrapped.draw(canvas)
                // Khôi phục trạng thái canvas
                canvas.restore()
            }

            override fun setAlpha(alpha: Int) {
                wrapped.alpha = alpha
            }

            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                wrapped.colorFilter = colorFilter
            }

            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int {
                return wrapped.opacity
            }

            override fun getIntrinsicWidth(): Int = wrapped.intrinsicWidth
            override fun getIntrinsicHeight(): Int = wrapped.intrinsicHeight
        }
    }

    init {
        themeResId = if (darkMode) R.style.dialog_full_screen_dark else R.style.dialog_full_screen_light
    }
}
