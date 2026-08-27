package com.omarea.common.ui

import android.app.Activity
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
    // Lưu ý: class này phải khai báo BÊN NGOÀI companion object (dù chỉ được tạo ra từ hàm trong
    // companion object bên dưới). Kotlin không tự "nâng" 1 class lồng BÊN TRONG companion object
    // thành DialogFullScreen.TenClass như với hàm/property - phải gọi là
    // DialogFullScreen.Companion.TenClass, khiến mọi nơi gọi từ file khác
    // (DialogLogFragment.kt, ActionListFragment.kt, DialogHelper.kt) bị lỗi build "Unresolved
    // reference" vì chỉ viết DialogFullScreen.SwipeToDismissBinding.
    class SwipeToDismissBinding internal constructor(
        private val helper: DialogSwipeBackHelper,
        private val predictiveBackCallback: Any?
    ) {
        fun release(dialog: Dialog) {
            DialogPredictiveBackBinder.unbind(dialog, predictiveBackCallback)
            helper.release()
        }
    }

    companion object {
        /**
         * Gắn cử chỉ vuốt lùi (vuốt sang phải để đóng, + vuốt-từ-mép predictive-back API 33+)
         * cho 1 Dialog TOÀN MÀN HÌNH bất kỳ ĐÃ show (dialog.window khác null, đã setContentView)
         * - dùng chung cho chính DialogFullScreen (bên dưới, xem onViewCreated()) LẪN các dialog
         * dựng tay khác không kế thừa DialogFullScreen, ví dụ dialog tham số
         * (kr_dialog_params/kr_dialog_params_small) ở ActionListFragment, dialog blur ở
         * DialogHelper.customDialog(), hay DialogLogFragment - kể cả dialog dựng qua AlertDialog
         * (không chỉ Dialog thường), vì DialogSwipeBackBlurWrapper.wrap() giờ tự lấy nội dung
         * thật từ android.R.id.content của window, không cần bên gọi tự chỉ view nào.
         *
         * @param activity Activity đang chứa dialog (cần để FastBlurUtility chụp màn hình phía
         * sau, xem DialogSwipeBackBlurWrapper).
         * @param dialog Dialog ĐÃ show (đã setContentView - với DialogFragment nghĩa là gọi SAU
         * onActivityCreated(), ví dụ trong view.post {} từ onViewCreated()).
         * @param onBack callback khi vuốt lùi hoàn tất - thường là dialog.dismiss().
         * @return handle để bên gọi release() đúng lúc dialog bị dismiss/destroy (tránh leak
         * VelocityTracker/animator + hủy đăng ký predictive-back), null nếu dialog chưa có
         * window.
         */
        fun bindSwipeToDismiss(activity: Activity, dialog: Dialog, onBack: () -> Unit): SwipeToDismissBinding? {
            val window = dialog.window ?: return null
            val swipeTarget = DialogSwipeBackBlurWrapper.wrap(activity, window) ?: run {
                DialogHelper.setWindowBlurBg(window, activity)
                // Fallback: không bọc blur trượt được thì vẫn kéo được cả khối android.R.id.content
                // (toàn bộ nội dung window) - chỉ là nền blur phía dưới đứng yên tĩnh như hành vi
                // cũ, không trượt cùng.
                window.findViewById(android.R.id.content)
            }
            val helper = DialogSwipeBackHelper.bind(dialog, swipeTarget) { onBack() } ?: return null
            val predictiveBackCallback = DialogPredictiveBackBinder.bind(dialog, helper)
            return SwipeToDismissBinding(helper, predictiveBackCallback)
        }
    }

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
    private var swipeToDismissBinding: SwipeToDismissBinding? = null

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

            if (swipeToDismissEnabled && isCancelable) {
                // isCancelable=false (xem androidx.fragment.app.DialogFragment) nghĩa là dialog
                // này KHÔNG cho phép đóng bằng nút back/chạm ra ngoài -> vuốt lùi cũng phải bị
                // chặn theo, không thì người dùng vẫn có 1 đường lách để đóng dialog "không thể
                // đóng" này.
                // QUAN TRỌNG: view.parent vẫn còn null tại đây - AndroidX DialogFragment chỉ
                // thật sự gọi dialog.setContentView(view) ở onActivityCreated() (chạy SAU
                // onViewCreated()), nên DialogSwipeBackBlurWrapper.wrap() gọi ngay tại chỗ này
                // sẽ luôn thấy parent null và fallback về nền blur tĩnh cũ. view.post() đợi 1
                // vòng của UI thread - lúc đó setContentView() đã chạy xong, view đã có parent.
                view.post {
                    if (d.window == null) return@post
                    swipeToDismissBinding = bindSwipeToDismiss(activity, d) { closeView() }
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
        dialog?.let { swipeToDismissBinding?.release(it) }
        swipeToDismissBinding = null
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