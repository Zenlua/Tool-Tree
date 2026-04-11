package com.tool.tree

import android.app.Activity
import android.view.View
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.ui.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DialogPower(private val activity: Activity) {

    fun showPowerMenu() {
        val layoutInflater = activity.layoutInflater
        val view = layoutInflater.inflate(R.layout.dialog_power_operation, null)
        val dialog = DialogHelper.customDialog(activity, view)

        // Hàm helper để tránh lặp code
        fun runShellCommand(resId: Int) {
            dialog.dismiss()
            val cmd = activity.getString(resId)
            // Chạy ngầm để tránh treo máy (ANR)
            GlobalScope.launch(Dispatchers.IO) {
                KeepShellPublic.doCmdSync(cmd)
            }
        }

        view.apply {
            findViewById<View>(R.id.power_shutdown).setOnClickListener {
                runShellCommand(R.string.power_shutdown_cmd)
            }
            findViewById<View>(R.id.power_reboot).setOnClickListener {
                runShellCommand(R.string.power_reboot_cmd)
            }
            findViewById<View>(R.id.power_hot_reboot).setOnClickListener {
                runShellCommand(R.string.power_download_cmd)
            }
            findViewById<View>(R.id.power_recovery).setOnClickListener {
                runShellCommand(R.string.power_recovery_cmd)
            }
            findViewById<View>(R.id.power_fastboot).setOnClickListener {
                runShellCommand(R.string.power_fastboot_cmd)
            }
            findViewById<View>(R.id.power_emergency).setOnClickListener {
                runShellCommand(R.string.power_emergency_cmd)
            }
        }
    }
}
