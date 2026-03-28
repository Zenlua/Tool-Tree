package com.tool.tree

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ToastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Lấy dữ liệu từ intent
        val message = intent.getStringExtra("text")
        
        // Hiển thị Toast với nội dung nhận được từ broadcast
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}