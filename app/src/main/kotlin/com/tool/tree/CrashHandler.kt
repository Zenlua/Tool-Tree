package com.tool.tree

import android.content.Context
import android.content.Intent
import android.util.Log

class CrashHandler(context: Context) : Thread.UncaughtExceptionHandler {

    private val context: Context = context.applicationContext
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler() // ← Lưu default handler

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            val stackTrace = Log.getStackTraceString(ex)
            Log.e("CrashHandler", "Uncaught exception in thread: " + thread.name, ex)

            // (Tùy chọn) Lưu log vào file nếu bạn có implement
            // CrashFileWriter.write(context, stackTrace);

            val intent = Intent(context, CrashLogActivity::class.java)
            intent.putExtra("crash_log", stackTrace)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )

            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e("CrashHandler", "Failed to handle crash gracefully", t)
        }

        // Luôn gọi default handler ở đây (ngoài try-catch) để:
        // - Đảm bảo logcat có stack trace chuẩn (tag AndroidRuntime)
        // - Process được kill đúng cách
        // - Dialog "App đã dừng" xuất hiện nếu startActivity fail hoặc bạn muốn fallback
        defaultHandler?.uncaughtException(thread, ex)
    }

    companion object {
        // Phương thức tiện lợi để cài đặt (gọi 1 lần trong Application.onCreate)
        @JvmStatic
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
        }
    }
}
