package com.omarea.common.shell.shizuku

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.omarea.common.shell.ShellSession
import com.omarea.common.shell.ShellTranslation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

/**
 * Cầu nối phía app tới ShellUserService (chạy trong tiến trình Shizuku cấp). Mức ưu tiên nguồn
 * thực thi trong toàn app: su > Shizuku > sh thường - xem SplashActivity.checkRootAndStart() và
 * KeepShellPublic, đây chỉ là lớp cung cấp nguồn Shizuku, không tự quyết định thứ tự ưu tiên.
 */
object ShizukuShellManager {
    private const val BIND_TIMEOUT_MS = 8000L
    private const val PERMISSION_REQUEST_TIMEOUT_MS = 30000L
    private const val REQUEST_CODE_STARTUP = 24601

    @Volatile
    private var binder: IShellUserService? = null

    @Volatile
    private var bound = false
    private val bindLock = Object()

    private fun userServiceArgs(context: Context) = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShellUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku")
        .debuggable(false)
        .version(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(bindLock) {
                binder = if (service != null && service.pingBinder()) {
                    IShellUserService.Stub.asInterface(service)
                } else {
                    null
                }
                bound = true
                bindLock.notifyAll()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            synchronized(bindLock) {
                binder = null
                bound = false
            }
        }
    }

    // Shizuku (app quản lý Shizuku hoặc Sui) đã cài và service đang chạy chưa.
    fun isInstalled(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    // App đã được cấp quyền Shizuku chưa (quyền được cấp/thu hồi qua app quản lý Shizuku).
    fun hasPermission(): Boolean {
        return try {
            isInstalled() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // Chỉ nên gọi từ 1 màn hình Cài đặt do người dùng chủ động bấm - hiển thị popup xin quyền hệ
    // thống của Shizuku. Đăng ký Shizuku.addRequestPermissionResultListener ở nơi gọi để nhận kết
    // quả (requestCode do nơi gọi tự chọn).
    fun requestPermission(requestCode: Int) {
        try {
            if (isInstalled()) Shizuku.requestPermission(requestCode)
        } catch (_: Exception) {
        }
    }

    // Dùng ở bước khởi động app, SAU KHI người dùng đã bấm xác nhận (đồng ý điều khoản/quyền) -
    // hiện popup xin quyền hệ thống của Shizuku (nếu Shizuku đã cài + chưa có quyền) và CHỜ kết
    // quả (tối đa PERMISSION_REQUEST_TIMEOUT_MS) thay vì âm thầm bỏ qua như tryUseGrantedSession().
    // Trả về true nếu đã có/vừa được cấp quyền, false nếu Shizuku chưa cài, người dùng từ chối,
    // hoặc hết thời gian chờ (ví dụ App Shizuku hiện popup nhưng người dùng không thao tác).
    suspend fun requestPermissionAndAwait(activity: Activity): Boolean {
        if (hasPermission()) return true
        if (!isInstalled()) return false
        return try {
            withTimeoutOrNull(PERMISSION_REQUEST_TIMEOUT_MS) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val listener = object : Shizuku.OnRequestPermissionResultListener {
                        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                            if (requestCode != REQUEST_CODE_STARTUP) return
                            Shizuku.removeRequestPermissionResultListener(this)
                            if (cont.isActive) {
                                cont.resumeWith(Result.success(grantResult == PackageManager.PERMISSION_GRANTED))
                            }
                        }
                    }
                    Shizuku.addRequestPermissionResultListener(listener)
                    cont.invokeOnCancellation {
                        try {
                            Shizuku.removeRequestPermissionResultListener(listener)
                        } catch (_: Exception) {
                        }
                    }
                    activity.runOnUiThread {
                        try {
                            Shizuku.requestPermission(REQUEST_CODE_STARTUP)
                        } catch (e: Exception) {
                            Shizuku.removeRequestPermissionResultListener(listener)
                            if (cont.isActive) cont.resumeWith(Result.success(false))
                        }
                    }
                }
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Dùng lúc khởi động app (SplashActivity): KHÔNG hiện popup xin quyền, chỉ trả về phiên làm
    // việc nếu Shizuku đã cài, đang chạy VÀ đã được cấp quyền từ trước. Trả về null trong mọi
    // trường hợp còn lại để nơi gọi tự fallback sang sh thường - không làm chậm khởi động ở máy
    // không dùng Shizuku.
    fun tryUseGrantedSession(context: Context): ShellSession? {
        if (!hasPermission()) return null
        val currentBinder = bindSession(context) ?: return null
        return ShellSessionShizuku(currentBinder)
    }

    @Synchronized
    private fun bindSession(context: Context): IShellUserService? {
        synchronized(bindLock) {
            if (bound && binder != null) return binder
        }
        try {
            Shizuku.bindUserService(userServiceArgs(context.applicationContext), connection)
            synchronized(bindLock) {
                if (!bound) bindLock.wait(BIND_TIMEOUT_MS)
            }
        } catch (_: Exception) {
            return null
        }
        return binder
    }

    fun unbind(context: Context) {
        try {
            Shizuku.unbindUserService(userServiceArgs(context.applicationContext), connection, true)
        } catch (_: Exception) {
        }
        synchronized(bindLock) {
            binder = null
            bound = false
        }
    }
}

/**
 * Bọc IShellUserService (Binder) thành ShellSession để KeepShellPublic/ScriptEnvironmen dùng
 * chung kiểu với KeepShell (su). Marker protocol/đồng bộ hóa nằm bên trong ShellUserService
 * (chạy ở tiến trình Shizuku cấp), ở đây chỉ chuyển tiếp lệnh qua Binder.
 */
class ShellSessionShizuku(private val binder: IShellUserService) : ShellSession {
    // Không giữ pool nhiều tiến trình như KeepShell (su) - lệnh được xếp hàng bởi khóa bên trong
    // ShellUserService nên luôn coi là "rảnh" để KeepShellPublic không cần phiên thứ 2.
    override val isIdle: Boolean = true

    override fun doCmdSync(cmd: String): String {
        return try {
            binder.execCommand(cmd) ?: "error"
        } catch (e: Exception) {
            "error"
        }
    }

    override fun doCmdSync(shellCommand: String, shellTranslation: ShellTranslation): String {
        val rows = doCmdSync(shellCommand).split("\n")
        return if (rows.isNotEmpty()) shellTranslation.resolveRows(rows) else ""
    }

    override fun checkRoot(): Boolean {
        return doCmdSync("id -u").trim() == "0"
    }

    override fun tryExit() {
        try {
            binder.destroy()
        } catch (_: Exception) {
        }
    }
}
