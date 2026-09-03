package com.omarea.krscript.executor

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.omarea.common.shared.FileWrite
import com.omarea.common.shell.KeepShell
import com.omarea.common.shell.KeepShellPublic
import com.omarea.common.shell.ShellTranslation
import com.omarea.krscript.FileOwner
import com.omarea.krscript.model.NodeInfoBase
import com.tool.tree.ThemeModeState
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.TimeZone

object ScriptEnvironmen {
    private const val ASSETS_FILE = "file:///android_asset/"
    private var inited = false
    private var environmentPath = ""
    private var TOOKIT_DIR = ""
    private var rooted = false
    private var privateShell: KeepShell? = null
    private var shellTranslation: ShellTranslation? = null

    // Template gốc của executor.sh (chưa thay thế biến), lưu lại để có thể build lại
    // file executor mỗi khi các biến môi trường "động" (vd: DARK_MODE) thay đổi.
    private var envShellTemplate = ""
    private var executorFileName = ""

    // Giá trị DARK_MODE đã được ghi vào file executor gần nhất, dùng để tránh ghi lại
    // file khi giá trị không đổi (switchTheme có thể được gọi ở onCreate của mọi Activity).
    private var lastDarkMode: Boolean? = null

    @JvmStatic
    fun isInited(): Boolean {
        return inited
    }

    private fun init(context: Context): Boolean {
        val configSpf = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE)

        return init(context, configSpf.getString("executor", "root/executor.sh"), configSpf.getString("toolkitDir", "home"))
    }

    @JvmStatic
    fun init(context: Context, executor: String?, toolkitDir: String?): Boolean {
        if (inited) {
            return true
        }

        shellTranslation = ShellTranslation(context.applicationContext)
        rooted = KeepShellPublic.checkRoot()

        try {
            if (!toolkitDir.isNullOrEmpty()) {
                TOOKIT_DIR = ExtractAssets(context).extractResources(toolkitDir) ?: ""
            }

            var fileName = executor ?: ""
            if (fileName.startsWith(ASSETS_FILE)) {
                fileName = fileName.substring(ASSETS_FILE.length)
            }

            val inputStream = context.assets.open(fileName)
            val bytes = ByteArray(inputStream.available())
            val length = inputStream.read(bytes, 0, bytes.size)
            val envShell = String(bytes, Charset.defaultCharset()).replace("\r", "")

            // Lưu lại template gốc (trước khi thay thế biến) và tên file, để sau này
            // có thể build lại file executor khi cần cập nhật các biến "động" như DARK_MODE.
            envShellTemplate = envShell
            executorFileName = fileName

            inited = writeExecutorScript(context)
            if (inited) {
                lastDarkMode = ThemeModeState.isDarkMode()
            }

            val configSpf = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE).edit()
            configSpf.putString("executor", executor)
            configSpf.putString("toolkitDir", toolkitDir)
            configSpf.apply()

            privateShell = if (rooted) KeepShellPublic.getDefaultInstance() else KeepShell(rooted)

            return inited
        } catch (ex: Exception) {
            return false
        }
    }

    // Build lại nội dung executor.sh từ template gốc (envShellTemplate) với các biến
    // môi trường mới nhất (bao gồm DARK_MODE), rồi ghi đè xuống file private.
    // Tách riêng khỏi init() để có thể gọi lại nhiều lần trong vòng đời app.
    private fun writeExecutorScript(context: Context): Boolean {
        if (envShellTemplate.isEmpty() || executorFileName.isEmpty()) {
            return false
        }
        try {
            var envShell = envShellTemplate

            val environment = getEnvironment(context)
            for (key in environment.keys) {
                val value = environment[key] ?: ""
                envShell = envShell.replace("\$({$key})", value)
            }
            val outputPathAbs = FileWrite.getPrivateFilePath(context, executorFileName)
            envShell = envShell.replace("\$({EXECUTOR_PATH})", outputPathAbs)

            val success = FileWrite.writePrivateFile(envShell.toByteArray(Charset.defaultCharset()), executorFileName, context)
            if (success) {
                environmentPath = outputPathAbs
            }
            return success
        } catch (ex: Exception) {
            return false
        }
    }

    // Gọi hàm này mỗi khi chế độ dark mode của app thay đổi (vd: trong
    // ThemeModeState.switchTheme) để cập nhật lại biến DARK_MODE trong file executor.
    // Trước đây DARK_MODE chỉ được tính 1 lần lúc init() nên khi đổi dark mode ở giữa
    // phiên sử dụng, giá trị cũ vẫn được giữ nguyên cho tới khi khởi động lại app.
    @JvmStatic
    @Synchronized
    fun updateDarkMode(context: Context, isDarkMode: Boolean): Boolean {
        if (!inited) {
            // Chưa init thì giá trị DARK_MODE sẽ được lấy đúng ở lần init() đầu tiên,
            // không cần làm gì thêm ở đây.
            return false
        }
        if (lastDarkMode != null && lastDarkMode == isDarkMode) {
            // Giá trị không đổi, không cần ghi lại file.
            return true
        }
        val success = writeExecutorScript(context)
        if (success) {
            lastDarkMode = isDarkMode
        }
        return success
    }

    private fun md5(string: String): String {
        if (string.isEmpty()) {
            return ""
        }

        val md5: MessageDigest
        try {
            md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest(string.toByteArray())
            val result = StringBuilder()
            for (b in bytes) {
                var temp = Integer.toHexString(b.toInt() and 0xff)
                if (temp.length == 1) {
                    temp = "0$temp"
                }
                result.append(temp)
            }
            return result.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return ""
    }

    private fun createShellCache(context: Context, script: String): String {
        val md5 = md5(script)
        val relativePath = "root/$md5.sh"
        val absolutePath = FileWrite.getPrivateFilePath(context, relativePath)
        if (File(absolutePath).exists()) {
            return absolutePath
        }
        val bytes = ("#!/data/data/com.tool.tree/files/home/bin/bash\n\n$script").toByteArray()

        if (FileWrite.writePrivateFile(bytes, relativePath, context)) {
            return absolutePath
        }
        return ""
    }

    private fun extractScript(context: Context, fileNameArg: String): String? {
        var fileName = fileNameArg
        if (fileName.startsWith(ASSETS_FILE)) {
            fileName = fileName.substring(ASSETS_FILE.length)
        }
        return FileWrite.writePrivateShellFile(fileName, fileName, context)
    }

    @JvmStatic
    @JvmOverloads
    fun executeResultRoot(context: Context, script: String?, nodeInfoBase: NodeInfoBase?, extraParams: HashMap<String, String>? = null): String {
        if (!inited) {
            init(context)
        }

        if (script.isNullOrEmpty()) {
            return ""
        }

        val script2 = script.trim()
        val path: String
        path = if (script2.startsWith(ASSETS_FILE)) {
            extractScript(context, script2) ?: ""
        } else {
            createShellCache(context, script)
        }

        if (!inited) {
            init(context)
        }

        val stringBuilder = StringBuilder()

        stringBuilder.append("\n")
        if (nodeInfoBase != null && nodeInfoBase.currentPageConfigPath.isNotEmpty()) {
            val parentPageConfigDir = nodeInfoBase.pageConfigDir
            val currentPageConfigPath = nodeInfoBase.currentPageConfigPath
            stringBuilder.append("export PAGE_CONFIG_DIR='").append(parentPageConfigDir).append("'\n")
            stringBuilder.append("export PAGE_CONFIG_FILE='").append(currentPageConfigPath).append("'\n")

            if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                stringBuilder.append("export PAGE_WORK_DIR='").append(ExtractAssets(context).getExtractPath(parentPageConfigDir)).append("'\n")
                stringBuilder.append("export PAGE_WORK_FILE='").append(ExtractAssets(context).getExtractPath(currentPageConfigPath)).append("'\n")
            } else {
                stringBuilder.append("export PAGE_WORK_DIR='").append(parentPageConfigDir).append("'\n")
                stringBuilder.append("export PAGE_WORK_FILE='").append(currentPageConfigPath).append("'\n")
            }
        }

        if (extraParams != null) {
            for ((key, v) in extraParams) {
                val value = v?.replace("'", "'\\''") ?: ""
                stringBuilder.append("export ").append(key).append("='").append(value).append("'\n")
            }
        }

        stringBuilder.append("\n\n")
        stringBuilder.append("$environmentPath \"$path\"")
        val shell = privateShell
        return if (shellTranslation != null && shell != null) {
            shellTranslation!!.resolveRow(shell.doCmdSync(stringBuilder.toString()))
        } else {
            shell?.doCmdSync(stringBuilder.toString()) ?: ""
        }
    }

    // ========== TỐI ƯU: GỘP NHIỀU SCRIPT THÀNH 1 LẦN GỌI SHELL DUY NHẤT ==========
    // Dùng khi cần đọc value/options của NHIỀU ActionParam cùng lúc (ví dụ khi mở dialog
    // nhập tham số của 1 action có nhiều param, mỗi param có thể có valueShell/optionsSh
    // riêng). Trước đây mỗi script được gọi qua 1 lần executeResultRoot() -> 1 round-trip
    // riêng qua shell root (vốn là 1 tiến trình DÙNG CHUNG, có khóa ReentrantLock, nên các
    // lệnh luôn phải xếp hàng chạy TUẦN TỰ dù gọi bằng coroutine song song). Với N script,
    // cách cũ tốn N lần ghi/đọc qua BufferedReader + N lần kiểm tra/tạo cache file (MD5 +
    // File.exists()).
    //
    // Hàm này gộp toàn bộ N script thành 1 khối lệnh duy nhất (mỗi script vẫn được cache
    // ra file như cũ, chỉ gộp lúc GỌI shell), bọc mỗi script bằng 1 marker echo riêng dựa
    // trên hash của tag, rồi gọi doCmdSync() ĐÚNG 1 LẦN. Sau đó tách kết quả theo marker để
    // trả về map tag -> kết quả (giữ đúng ngữ nghĩa như executeResultRoot cho từng script).
    //
    // scripts: key = tag định danh duy nhất do caller tự đặt (ví dụ "value:tenParam",
    //          "options:tenParam"), value = nội dung script (null/rỗng sẽ được bỏ qua,
    //          trả về "" cho tag đó, KHÔNG tốn round-trip).
    @JvmStatic
    fun executeMultipleResultRoot(
        context: Context,
        scripts: LinkedHashMap<String, String>?,
        nodeInfoBase: NodeInfoBase?
    ): LinkedHashMap<String, String> {
        val results = LinkedHashMap<String, String>()
        if (scripts.isNullOrEmpty()) {
            return results
        }

        if (!inited) {
            init(context)
        }

        // Lọc bỏ script rỗng/null ngay từ đầu, không tốn chỗ trong lệnh gộp
        val validScripts = LinkedHashMap<String, String>()
        for ((key, script) in scripts) {
            if (script.trim().isNotEmpty()) {
                validScripts[key] = script
            } else {
                results[key] = ""
            }
        }
        if (validScripts.isEmpty()) {
            return results
        }

        // Chỉ có 1 script hợp lệ thì không cần gộp, dùng thẳng hàm cũ cho đơn giản
        if (validScripts.size == 1) {
            val only = validScripts.entries.iterator().next()
            results[only.key] = executeResultRoot(context, only.value, nodeInfoBase)
            return results
        }

        val cmd = StringBuilder()
        cmd.append("\n")

        // Các biến môi trường phụ thuộc trang (PAGE_CONFIG_DIR...) chỉ cần export 1 LẦN
        // cho cả khối lệnh gộp, thay vì lặp lại cho từng script như trước.
        if (nodeInfoBase != null && nodeInfoBase.currentPageConfigPath.isNotEmpty()) {
            val parentPageConfigDir = nodeInfoBase.pageConfigDir
            val currentPageConfigPath = nodeInfoBase.currentPageConfigPath
            cmd.append("export PAGE_CONFIG_DIR='").append(parentPageConfigDir).append("'\n")
            cmd.append("export PAGE_CONFIG_FILE='").append(currentPageConfigPath).append("'\n")

            if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                cmd.append("export PAGE_WORK_DIR='").append(ExtractAssets(context).getExtractPath(parentPageConfigDir)).append("'\n")
                cmd.append("export PAGE_WORK_FILE='").append(ExtractAssets(context).getExtractPath(currentPageConfigPath)).append("'\n")
            } else {
                cmd.append("export PAGE_WORK_DIR='").append(parentPageConfigDir).append("'\n")
                cmd.append("export PAGE_WORK_FILE='").append(currentPageConfigPath).append("'\n")
            }
        }
        cmd.append("\n")

        val orderedTags = ArrayList(validScripts.keys)

        // ===== TỐI ƯU (v2) =====
        // Bản gộp trước chỉ gộp được 1 ROUND-TRIP qua doCmdSync(), nhưng bên TRONG round-trip
        // đó vẫn gọi "environmentPath <path>" RIÊNG cho từng script -> mỗi lần gọi là 1 TIẾN
        // TRÌNH MỚI phải chạy lại toàn bộ nội dung executor.sh (export TOOLKIT, START_DIR,
        // PATH...) dù các biến này giống hệt nhau giữa các script trong cùng 1 lần refresh.
        // Với N checkbox, tốn N lần spawn + setup lại executor -> đây mới là phần thực sự
        // gây chậm (không phải bản thân lệnh getprop), nên N càng lớn càng chậm rõ dù chỉ có
        // 1 round-trip qua shell.
        //
        // Cách khắc phục: gộp toàn bộ N script (kèm marker echo) thành 1 FILE DUY NHẤT, rồi
        // chỉ gọi "environmentPath <mergedPath>" ĐÚNG 1 LẦN. executor.sh chỉ setup môi trường
        // 1 LẦN rồi thực thi mergedPath; bên trong mergedPath mỗi script con được bọc trong
        // "( ... )" - 1 subshell nhẹ (chỉ fork, KHÔNG chạy lại executor) để cô lập cd/exit của
        // từng script với nhau, giữ đúng ngữ nghĩa cũ.
        val merged = StringBuilder()
        for (tag in orderedTags) {
            val script = validScripts[tag]!!
            val script2 = script.trim()
            val marker = "KRBATCH_" + md5(tag)

            merged.append("echo '>>>").append(marker).append("'\n")
            merged.append("(\n")
            if (script2.startsWith(ASSETS_FILE)) {
                val assetPath = extractScript(context, script2)
                if (!assetPath.isNullOrEmpty()) {
                    merged.append(". \"").append(assetPath).append("\"\n")
                }
            } else {
                merged.append(script2).append("\n")
            }
            merged.append(")\n")
            merged.append("echo '<<<").append(marker).append("'\n")
        }

        val mergedPath = createShellCache(context, merged.toString())
        if (mergedPath.isNotEmpty()) {
            cmd.append(environmentPath).append(" \"").append(mergedPath).append("\"\n")
        }

        var rawOutput = privateShell?.doCmdSync(cmd.toString()) ?: ""
        if (shellTranslation != null) {
            rawOutput = shellTranslation!!.resolveRow(rawOutput)
        }

        // Tách kết quả gộp thành từng phần theo marker của mỗi tag
        for (tag in orderedTags) {
            val marker = "KRBATCH_" + md5(tag)
            val startMarker = ">>>$marker"
            val endMarker = "<<<$marker"
            val startIdx = rawOutput.indexOf(startMarker)
            val endIdx = rawOutput.indexOf(endMarker)
            if (startIdx >= 0 && endIdx > startIdx) {
                val section = rawOutput.substring(startIdx + startMarker.length, endIdx)
                results[tag] = section.trim()
            } else {
                results[tag] = "error"
            }
        }

        return results
    }

    // Giống executeResultRoot(), nhưng gọi onLine(dòng) NGAY KHI shell xuất ra 1 dòng output
    // mới (qua KeepShell.doCmdStreaming), thay vì đợi toàn bộ script chạy xong mới trả kết
    // quả. Dùng cho PageConfigSh.executeStreaming() (trang có process=true). Mỗi dòng vẫn
    // được dịch qua shellTranslation giống hệt executeResultRoot (chỉ khác là dịch theo từng
    // dòng thay vì dịch 1 lần trên toàn bộ chuỗi).
    @JvmStatic
    @JvmOverloads
    fun executeStreamingRoot(
        context: Context,
        script: String?,
        nodeInfoBase: NodeInfoBase?,
        extraParams: HashMap<String, String>? = null,
        onLine: (String) -> Unit
    ): String {
        if (!inited) {
            init(context)
        }

        if (script.isNullOrEmpty()) {
            return ""
        }

        val script2 = script.trim()
        val path: String
        path = if (script2.startsWith(ASSETS_FILE)) {
            extractScript(context, script2) ?: ""
        } else {
            createShellCache(context, script)
        }

        if (!inited) {
            init(context)
        }

        val stringBuilder = StringBuilder()

        stringBuilder.append("\n")
        if (nodeInfoBase != null && nodeInfoBase.currentPageConfigPath.isNotEmpty()) {
            val parentPageConfigDir = nodeInfoBase.pageConfigDir
            val currentPageConfigPath = nodeInfoBase.currentPageConfigPath
            stringBuilder.append("export PAGE_CONFIG_DIR='").append(parentPageConfigDir).append("'\n")
            stringBuilder.append("export PAGE_CONFIG_FILE='").append(currentPageConfigPath).append("'\n")

            if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                stringBuilder.append("export PAGE_WORK_DIR='").append(ExtractAssets(context).getExtractPath(parentPageConfigDir)).append("'\n")
                stringBuilder.append("export PAGE_WORK_FILE='").append(ExtractAssets(context).getExtractPath(currentPageConfigPath)).append("'\n")
            } else {
                stringBuilder.append("export PAGE_WORK_DIR='").append(parentPageConfigDir).append("'\n")
                stringBuilder.append("export PAGE_WORK_FILE='").append(currentPageConfigPath).append("'\n")
            }
        }

        if (extraParams != null) {
            for ((key, v) in extraParams) {
                val value = v?.replace("'", "'\\''") ?: ""
                stringBuilder.append("export ").append(key).append("='").append(value).append("'\n")
            }
        }

        stringBuilder.append("\n\n")
        stringBuilder.append("$environmentPath \"$path\"")

        val shell = privateShell ?: return ""
        val translation = shellTranslation
        val resultBuilder = StringBuilder()
        shell.doCmdStreaming(stringBuilder.toString()) { rawLine ->
            val line = if (translation != null) translation.resolveRow(rawLine) else rawLine
            if (resultBuilder.isNotEmpty()) resultBuilder.append("\n")
            resultBuilder.append(line)
            onLine(line)
        }
        return resultBuilder.toString()
    }

    private fun getStartPath(context: Context): String {
        val dir = FileWrite.getPrivateFileDir(context)
        if (dir.endsWith("/")) {
            return dir.substring(0, dir.length - 1)
        }
        return dir
    }

    private fun getEnvironment(context: Context): HashMap<String, String> {
        val params = HashMap<String, String>()

        params["TOOLKIT"] = TOOKIT_DIR
        params["START_DIR"] = getStartPath(context)
        params["TEMP_DIR"] = context.cacheDir.absolutePath
        params["LANGUAGE"] = Locale.getDefault().language
        params["COUNTRY"] = Locale.getDefault().country
        params["TIMEZONE"] = TimeZone.getDefault().id
        params["ANDROID_RELEASE"] = Build.VERSION.RELEASE
        params["ANDROID_DEVICE"] = Build.DEVICE
        params["ANDROID_BRAND"] = Build.BRAND
        params["ANDROID_MANUFACTURER"] = Build.MANUFACTURER
        params["ANDROID_FINGERPRINT"] = Build.FINGERPRINT
        params["ANDROID_MODEL"] = Build.MODEL
        params["ANDROID_ID"] = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        @Suppress("DEPRECATION")
        params["CPU_ABI"] = Build.CPU_ABI
        params["ARCH"] = System.getProperty("os.arch") ?: ""
        params["ANDROID_SDK"] = Build.VERSION.SDK_INT.toString()
        params["KERNEL_VERSION"] = System.getProperty("os.version") ?: ""

        val fileOwner = FileOwner(context)
        val androidUid = fileOwner.getUserId()
        params["ANDROID_UID"] = androidUid.toString()

        try {
            params["APP_USER_ID"] = fileOwner.getFileOwner()
        } catch (ignored: Exception) {
        }

        try {
            params["DARK_MODE"] = if (ThemeModeState.isDarkMode()) "true" else "false"
        } catch (ignored: Exception) {
        }

        params["ROOT_PERMISSION"] = if (rooted) "true" else "false"
        params["SDCARD_PATH"] = Environment.getExternalStorageDirectory().absolutePath

        try {
            val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            params["PACKAGE_NAME"] = context.packageName
            params["PACKAGE_VERSION_NAME"] = packageInfo.versionName ?: ""
            params["PATH_APK"] = context.applicationInfo.sourceDir
            params["APP_UID"] = android.os.Process.myUid().toString()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params["PACKAGE_VERSION_CODE"] = packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                params["PACKAGE_VERSION_CODE"] = packageInfo.versionCode.toString()
            }
        } catch (ex: Exception) {
        }

        return params
    }

    private fun getVariables(params: HashMap<String, String>?): ArrayList<String> {
        val envp = ArrayList<String>()

        if (params != null) {
            for (key in params.keys) {
                val value = params[key] ?: ""
                envp.add(key + "='" + value.replace("'", "'\\''") + "'")
            }
        }

        return envp
    }

    private fun getExecuteScript(context: Context, script: String?, tag: String?): String {
        if (!inited) {
            init(context)
        }

        if (script.isNullOrEmpty()) {
            return ""
        }

        val script2 = script.trim()
        val cachePath: String = if (script2.startsWith(ASSETS_FILE)) {
            extractScript(context, script2) ?: script
        } else {
            createShellCache(context, script)
        }

        return "$environmentPath \"$cachePath\" \"$tag\""
    }

    @JvmStatic
    fun getRuntime(): Process? {
        return try {
            if (rooted) {
                try {
                    Runtime.getRuntime().exec("su")
                } catch (ignored: Exception) {
                    Runtime.getRuntime().exec("sh")
                }
            } else {
                Runtime.getRuntime().exec("sh")
            }
        } catch (ex: Exception) {
            null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun executeShell(
        context: Context,
        dataOutputStream: DataOutputStream,
        cmds: String?,
        paramsArg: HashMap<String, String>?,
        nodeInfo: NodeInfoBase?,
        tag: String?,
        needInput: Boolean = false
    ) {
        val params = paramsArg ?: HashMap()

        if (nodeInfo != null) {
            val parentPageConfigDir = nodeInfo.pageConfigDir
            val currentPageConfigPath = nodeInfo.currentPageConfigPath
            if (!parentPageConfigDir.isNullOrEmpty()) {
                params["PAGE_CONFIG_DIR"] = parentPageConfigDir
            }
            if (!currentPageConfigPath.isNullOrEmpty()) {
                params["PAGE_CONFIG_FILE"] = currentPageConfigPath
                if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                    val workDir = ExtractAssets(context).getExtractPath(parentPageConfigDir)
                    val workFile = ExtractAssets(context).getExtractPath(currentPageConfigPath)
                    if (!workDir.isNullOrEmpty()) {
                        params["PAGE_WORK_DIR"] = workDir
                    }
                    if (!workFile.isNullOrEmpty()) {
                        params["PAGE_WORK_FILE"] = workFile
                    }
                } else {
                    params["PAGE_WORK_DIR"] = parentPageConfigDir
                    params["PAGE_WORK_FILE"] = currentPageConfigPath
                }
            }
        }

        val envp = getVariables(params)
        val envpCmds = StringBuilder()
        if (envp.isNotEmpty()) {
            for (param in envp) {
                envpCmds.append("export ").append(param).append("\n")
            }
        }
        try {
            dataOutputStream.write(envpCmds.toString().toByteArray(StandardCharsets.UTF_8))

            val executeScript = getExecuteScript(context, cmds, tag)
            if (executeScript.isEmpty()) {
                return
            }
            if (needInput) {
                dataOutputStream.write(("$executeScript; sleep 0.2; exit\n").toByteArray(StandardCharsets.UTF_8))
            } else {
                dataOutputStream.write(("$executeScript\n\nsleep 0.2; exit\nexit\n").toByteArray(StandardCharsets.UTF_8))
            }
            dataOutputStream.flush()
        } catch (ignored: Exception) {
        }
    }
}
