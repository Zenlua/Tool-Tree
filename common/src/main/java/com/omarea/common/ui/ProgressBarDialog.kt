package com.omarea.common.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.omarea.common.R
import com.omarea.common.shell.AsynSuShellUnit

open class ProgressBarDialog(private var context: Activity, private var uniqueId: String? = null) {
    private var alert: DialogHelper.DialogWrap? = null
    private var textView: TextView? = null

    companion object {
        private val dialogs = LinkedHashMap<String, DialogHelper.DialogWrap>()
    }

    init {
        hideDialog()
    }

    class DefaultHandler(private var alertDialog: DialogHelper.DialogWrap?) : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            try {
                if (alertDialog == null) {
                    return
                }
                if (msg.what == 10) {
                    alertDialog!!.dismiss()
                    alertDialog!!.hide()
                    if (msg.obj == true) {
                        Toast.makeText(alertDialog!!.context, R.string.execute_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                    }
                } else if (msg.what == -1) {
                    Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                } else if (msg.what == 0 && msg.obj == false) {
                    alertDialog!!.dismiss()
                    alertDialog!!.hide()
                    Toast.makeText(alertDialog!!.context, R.string.execute_fail, Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun execShell(cmd: String, handler: Handler? = null) {
        hideDialog()
        val layoutInflater = LayoutInflater.from(context)
        val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
        val textView: TextView = (dialog.findViewById(R.id.dialog_text))
        textView.text = context.getString(R.string.execute_wait)
        alert = DialogHelper.customDialog(context, dialog, false)
        // AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        if (handler == null) {
            AsynSuShellUnit(DefaultHandler(alert)).exec(cmd).waitFor()
        } else {
            AsynSuShellUnit(handler).exec(cmd).waitFor()
        }
    }

    fun execShell(sb: StringBuilder, handler: Handler? = null) {
        execShell(sb.toString(), handler)
    }

    fun isDialogShow(): Boolean {
        return this.alert != null
    }

    fun hideDialog() {
        try {
            if (alert != null) {
                alert!!.dismiss()
                alert!!.hide()
                alert = null
            }
        } catch (_: Exception) {
        }

        uniqueId?.run {
            if (dialogs.containsKey(this)) {
                dialogs.remove(this)
            }
        }
    }

    fun showDialog(text: String = "正在加载，请稍等..."): ProgressBarDialog {
        if (textView != null && alert != null) {
            textView!!.text = text
        } else {
            hideDialog()
            val layoutInflater = LayoutInflater.from(context)
            val dialog = layoutInflater.inflate(R.layout.dialog_loading, null)
            textView = (dialog.findViewById(R.id.dialog_text)!!)
            textView!!.text = text
            alert = DialogHelper.customDialog(context, dialog, false)
            // AlertDialog.Builder(context).setView(dialog).setCancelable(false).create()
        }

        uniqueId?.run {
            if (dialogs.containsKey(this)) {
                dialogs.remove(this)
            }
            if (alert != null) {
                dialogs[this] = alert!!
            }
        }

        return this
    }
}
