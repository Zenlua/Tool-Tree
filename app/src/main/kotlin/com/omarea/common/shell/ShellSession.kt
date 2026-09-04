package com.omarea.common.shell

/**
 * Interface chung cho 1 phiên thực thi shell, bất kể chạy qua "su" nội bộ (KeepShell) hay qua
 * Shizuku (ShellSessionShizuku). Cho phép KeepShellPublic/ScriptEnvironmen dùng chung 1 kiểu mà
 * không cần biết đang chạy trên nguồn nào.
 */
interface ShellSession {
    val isIdle: Boolean
    fun doCmdSync(cmd: String): String
    fun doCmdSync(shellCommand: String, shellTranslation: ShellTranslation): String
    fun checkRoot(): Boolean
    fun tryExit()
}
