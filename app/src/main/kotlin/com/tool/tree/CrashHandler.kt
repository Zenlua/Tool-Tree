package com.tool.tree

import android.content.Context
import android.content.Intent
import android.util.Log

class CrashHandler(context: Context) : Thread.UncaughtExceptionHandler {

    private val context: Context = context.applicationContext
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler() // ← Lưu default handler

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val shownCrashUi = try {
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
            true
        } catch (t: Throwable) {
            Log.e("CrashHandler", "Failed to handle crash gracefully", t)
            false
        }

        if (shownCrashUi) {
            // CrashLogActivity chạy ở tiến trình riêng (android:process=":crash" trong
            // manifest) nên KHÔNG bị ảnh hưởng khi tiến trình hiện tại (đang crash) bị kill.
            //
            // QUAN TRỌNG: KHÔNG gọi defaultHandler ở nhánh này. Bản trước gọi
            // defaultHandler.uncaughtException() vô điều kiện ngay sau startActivity() - khi
            // đó CrashLogActivity còn chung tiến trình với app đang crash, nên bị handler mặc
            // định giết chết gần như ngay lập tức, dialog chỉ kịp flash lên rồi cả app crash
            // theo (đúng hiện tượng đã gặp). Giờ 2 tiến trình đã tách nhau, nhưng vẫn cố tình
            // không gọi defaultHandler nữa để tránh dialog hệ thống "App đã dừng" xuất hiện
            // chồng lên màn hình báo lỗi của chính app - tự dọn dẹp tiến trình đang crash
            // bằng cách kill trực tiếp, sạch sẽ và có kiểm soát hơn.
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        } else {
            // Không mở được CrashLogActivity (ví dụ lỗi ngay trong lúc build Intent/context) -
            // fallback về handler mặc định để ít nhất logcat có stack trace chuẩn (tag
            // AndroidRuntime) và hệ thống vẫn xử lý crash đúng cách (dialog "App đã dừng").
            defaultHandler?.uncaughtException(thread, ex)
        }
    }

    companion object {
        // Phương thức tiện lợi để cài đặt (gọi 1 lần trong Application.onCreate)
        @JvmStatic
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
        }
    }
}
