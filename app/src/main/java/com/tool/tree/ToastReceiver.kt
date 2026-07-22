package com.tool.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.omarea.krscript.config.StringResRef

class ToastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val raw = intent.getStringExtra("text") ?: return
        val message = StringResRef.resolve(context, raw)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}