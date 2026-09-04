package com.omarea.common.shell

/**
 * Created by Hello on 2018/01/23.
 */
object KeepShellPublic {
    private val keepShells = HashMap<String, ShellSession>()

    fun getInstance(key: String, rootMode: Boolean): ShellSession {
        synchronized(keepShells) {
            if (!keepShells.containsKey(key)) {
                keepShells[key] = KeepShell(rootMode)
            }
            return keepShells.get(key)!!
        }
    }

    fun destroyInstance(key: String) {
        synchronized(keepShells) {
            if (!keepShells.containsKey(key)) {
                return
            } else {
                val session = keepShells.get(key)!!
                keepShells.remove(key)
                session.tryExit()
            }
        }
    }

    fun destroyAll() {
        synchronized(keepShells) {
            while (keepShells.isNotEmpty()) {
                val key = keepShells.keys.first()
                val session = keepShells.get(key)!!
                keepShells.remove(key)
                session.tryExit()
            }
        }
    }

    // Mặc định 2 slot dùng KeepShell(su) - mức ưu tiên nguồn thực thi su > Shizuku > sh được
    // quyết định 1 lần lúc khởi động ở SplashActivity.checkRootAndStart(), gọi useSession() bên
    // dưới để chuyển sang Shizuku nếu su không khả dụng nhưng Shizuku có. Không gọi useSession()
    // thì giữ nguyên hành vi cũ (KeepShell tự fallback sh nếu không có su).
    private var defaultSession: ShellSession = KeepShell()
    private var secondarySession: ShellSession = KeepShell()

    fun getDefaultInstance(): ShellSession {
        return if (defaultSession.isIdle || !secondarySession.isIdle) {
            defaultSession
        } else {
            secondarySession
        }
    }

    // Chuyển 2 slot phiên làm việc dùng chung sang 1 nguồn thực thi khác (ví dụ ShellSessionShizuku).
    fun useSession(session: ShellSession) {
        val old1 = defaultSession
        val old2 = secondarySession
        defaultSession = session
        secondarySession = session
        old1.tryExit()
        if (old2 !== old1) old2.tryExit()
    }

    fun doCmdSync(commands: List<String>): Boolean {
        val stringBuilder = StringBuilder()

        for (cmd in commands) {
            stringBuilder.append(cmd)
            stringBuilder.append("\n\n")
        }

        return doCmdSync(stringBuilder.toString()) != "error"
    }

    //执行脚本
    fun doCmdSync(cmd: String): String {
        return getDefaultInstance().doCmdSync(cmd)
    }

    //执行脚本
    fun checkRoot(): Boolean {
        return defaultSession.checkRoot()
    }

    fun tryExit() {
        defaultSession.tryExit()
        if (secondarySession !== defaultSession) secondarySession.tryExit()
    }
}
