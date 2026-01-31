package com.omarea.common.ui

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.omarea.common.R
import androidx.fragment.app.DialogFragment

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

open class DialogFullScreen(
    private val layout: Int,
    darkMode: Boolean
) : DialogFragment() {

    init {
        setStyle(
            STYLE_NORMAL,
            if (darkMode)
                R.style.dialog_full_screen_dark
            else
                R.style.dialog_full_screen_light
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.let { window ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                window.setWindowAnimations(android.R.style.Animation_Translucent)
            }
            DialogHelper.setWindowBlurBg(window, requireActivity())
        }
    }

    fun closeView() {
        dismissAllowingStateLoss()
    }
}