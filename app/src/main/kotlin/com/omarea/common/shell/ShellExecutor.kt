package com.omarea.common.shell

import java.io.IOException

class ShellExecutor {
    companion object {
        private var extraEnvPath = ""
        private var defaultEnvPath = "" // /sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin

        // Thư mục tạm mà app CHẮC CHẮN có quyền ghi (khuyến nghị: context.getCacheDir().getAbsolutePath()).
        // Dùng để ép TMPDIR trỏ vào đây thay vì giá trị mặc định của hệ thống.
        private var extraTmpDir = ""

        @JvmStatic
        fun setExtraEnvPath(extraEnvPath: String) {
            Companion.extraEnvPath = extraEnvPath
        }

        // Gọi 1 lần lúc khởi động app, ví dụ:
        //   ShellExecutor.setTmpDir(context.getCacheDir().getAbsolutePath());
        @JvmStatic
        fun setTmpDir(tmpDir: String?) {
            extraTmpDir = tmpDir ?: ""
        }

        private fun getEnvPath(): String? {
            if (extraEnvPath.isNotEmpty()) {
                if (defaultEnvPath.isEmpty()) {
                    try {
                        val process = Runtime.getRuntime().exec("sh")
                        val outputStream = process.outputStream
                        outputStream.write("echo \$PATH".toByteArray())
                        outputStream.flush()
                        outputStream.close()

                        val inputStream = process.inputStream
                        val cache = ByteArray(16384)
                        val length = inputStream.read(cache)
                        inputStream.close()
                        process.destroy()

                        val path = String(cache, 0, length).trim()
                        if (path.isNotEmpty()) {
                            defaultEnvPath = path
                        } else {
                            throw RuntimeException("Unable to obtain \$PATH parameter")
                        }
                    } catch (ex: Exception) {
                        defaultEnvPath = "/system/bin:/vendor/bin:/odm/bin:/system/xbin:/vendor/xbin:/system/sbin:/sbin"
                    }
                }

                val path = defaultEnvPath

                return "PATH=$path:$extraEnvPath"
            }

            return null
        }

        // FIXED (trước đây là FIXME): ở chế độ non-root, biến TMPDIR mặc định của tiến trình
        // (thường là /data/local/tmp) app KHÔNG có quyền ghi -> các script dùng lệnh `source`,
        // `mktemp` hoặc bất kỳ thao tác nào cần ghi file tạm sẽ báo lỗi "Permission denied".
        // Ép TMPDIR trỏ về thư mục cache riêng của app (do setTmpDir() cung cấp, luôn ghi được
        // dù có root hay không) để tránh lỗi này. Nếu chưa gọi setTmpDir(), giữ nguyên hành vi
        // cũ (không export TMPDIR, để hệ thống tự quyết định).
        private fun buildEnvExportScript(): String? {
            val script = StringBuilder()

            val envPath = getEnvPath()
            if (envPath != null) {
                script.append("export ").append(envPath).append("\n")
            }

            if (extraTmpDir.isNotEmpty()) {
                script.append("export TMPDIR='").append(extraTmpDir).append("'\n")
            }

            return if (script.isNotEmpty()) script.toString() else null
        }

        @Throws(IOException::class)
        private fun getProcess(run: String): Process {
            val env = buildEnvExportScript()
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(run)
            if (env != null) {
                val outputStream = process.outputStream
                outputStream.write(env.toByteArray())
                outputStream.flush()
            }
            return process
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getSuperUserRuntime(): Process {
            return try {
                getProcess("su")
            } catch (e: IOException) {
                getProcess("sh")
            }
        }

        @JvmStatic
        @Throws(IOException::class)
        fun getRuntime(): Process {
            return getProcess("sh")
        }
    }
}
