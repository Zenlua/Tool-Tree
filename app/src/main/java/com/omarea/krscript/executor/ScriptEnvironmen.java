package com.omarea.krscript.executor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;

import com.omarea.common.shared.FileWrite;
import com.omarea.common.shell.KeepShell;
import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.FileOwner;
import com.omarea.krscript.model.NodeInfoBase;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.provider.Settings;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.TimeZone;
import java.lang.Runtime;
import java.nio.file.Paths;
import android.widget.Toast;
import com.tool.tree.ThemeModeState;

public class ScriptEnvironmen {
    private static final String ASSETS_FILE = "file:///android_asset/";
    private static boolean inited = false;
    private static String environmentPath = "";
    private static String TOOKIT_DIR = "";
    private static boolean rooted = false;
    private static KeepShell privateShell;
    private static ShellTranslation shellTranslation;
    // Template gốc của executor.sh (chưa thay thế biến), lưu lại để có thể build lại
    // file executor mỗi khi các biến môi trường "động" (vd: DARK_MODE) thay đổi.
    private static String envShellTemplate = "";
    private static String executorFileName = "";
    // Giá trị DARK_MODE đã được ghi vào file executor gần nhất, dùng để tránh ghi lại
    // file khi giá trị không đổi (switchTheme có thể được gọi ở onCreate của mọi Activity).
    private static Boolean lastDarkMode = null;

    public static boolean isInited() {
        return inited;
    }

    private static boolean init(Context context) {
        SharedPreferences configSpf = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE);

        return init(context, configSpf.getString("executor", "root/executor.sh"), configSpf.getString("toolkitDir", "home"));
    }

    public static boolean init(Context context, String executor, String toolkitDir) {
        if (inited) {
            return true;
        }

        shellTranslation = new ShellTranslation(context.getApplicationContext());
        rooted = KeepShellPublic.INSTANCE.checkRoot();

        try {
            if (toolkitDir != null && !toolkitDir.isEmpty()) {
                TOOKIT_DIR = new ExtractAssets(context).extractResources(toolkitDir);
            }

            String fileName = executor;
            if (fileName.startsWith(ASSETS_FILE)) {
                fileName = fileName.substring(ASSETS_FILE.length());
            }

            InputStream inputStream = context.getAssets().open(fileName);
            byte[] bytes = new byte[inputStream.available()];
            long length = inputStream.read(bytes, 0, bytes.length);
            String envShell = new String(bytes, Charset.defaultCharset()).replaceAll("\r", "");

            // Lưu lại template gốc (trước khi thay thế biến) và tên file, để sau này
            // có thể build lại file executor khi cần cập nhật các biến "động" như DARK_MODE.
            envShellTemplate = envShell;
            executorFileName = fileName;

            inited = writeExecutorScript(context);
            if (inited) {
                lastDarkMode = ThemeModeState.INSTANCE.isDarkMode();
            }

            SharedPreferences.Editor configSpf = context.getSharedPreferences("kr-script-config", Context.MODE_PRIVATE).edit();
            configSpf.putString("executor", executor);
            configSpf.putString("toolkitDir", toolkitDir);
            configSpf.apply();

            privateShell = rooted ? KeepShellPublic.INSTANCE.getDefaultInstance() : new KeepShell(rooted);

            return inited;
        } catch (Exception ex) {
            return false;
        }
    }

    // Build lại nội dung executor.sh từ template gốc (envShellTemplate) với các biến
    // môi trường mới nhất (bao gồm DARK_MODE), rồi ghi đè xuống file private.
    // Tách riêng khỏi init() để có thể gọi lại nhiều lần trong vòng đời app.
    private static boolean writeExecutorScript(Context context) {
        if (envShellTemplate.isEmpty() || executorFileName.isEmpty()) {
            return false;
        }
        try {
            String envShell = envShellTemplate;

            HashMap<String, String> environment = getEnvironment(context);
            for (String key : environment.keySet()) {
                String value = environment.get(key);
                if (value == null) {
                    value = "";
                }
                envShell = envShell.replace("$({" + key + "})", value);
            }
            String outputPathAbs = FileWrite.INSTANCE.getPrivateFilePath(context, executorFileName);
            envShell = envShell.replace("$({EXECUTOR_PATH})", outputPathAbs);

            boolean success = FileWrite.INSTANCE.writePrivateFile(envShell.getBytes(Charset.defaultCharset()), executorFileName, context);
            if (success) {
                environmentPath = outputPathAbs;
            }
            return success;
        } catch (Exception ex) {
            return false;
        }
    }

    // Gọi hàm này mỗi khi chế độ dark mode của app thay đổi (vd: trong
    // ThemeModeState.switchTheme) để cập nhật lại biến DARK_MODE trong file executor.
    // Trước đây DARK_MODE chỉ được tính 1 lần lúc init() nên khi đổi dark mode ở giữa
    // phiên sử dụng, giá trị cũ vẫn được giữ nguyên cho tới khi khởi động lại app.
    public static synchronized boolean updateDarkMode(Context context, boolean isDarkMode) {
        if (!inited) {
            // Chưa init thì giá trị DARK_MODE sẽ được lấy đúng ở lần init() đầu tiên,
            // không cần làm gì thêm ở đây.
            return false;
        }
        if (lastDarkMode != null && lastDarkMode == isDarkMode) {
            // Giá trị không đổi, không cần ghi lại file.
            return true;
        }
        boolean success = writeExecutorScript(context);
        if (success) {
            lastDarkMode = isDarkMode;
        }
        return success;
    }

    private static String md5(String string) {
        if (string.isEmpty()) {
            return "";
        }

        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String createShellCache(Context context, String script) {
        String md5 = md5(script);
        String relativePath = "root/" + md5 + ".sh";
        String absolutePath = FileWrite.INSTANCE.getPrivateFilePath(context, relativePath);
        if (new File(absolutePath).exists()) {
            return absolutePath;
        }
        byte[] bytes = ("#!/data/data/com.tool.tree/files/home/bin/bash\n\n" + script).getBytes();
        
        if (FileWrite.INSTANCE.writePrivateFile(bytes, relativePath, context)) {
            return absolutePath;
        }
        return "";
    }

    private static String extractScript(Context context, String fileName) {
        if (fileName.startsWith(ASSETS_FILE)) {
            fileName = fileName.substring(ASSETS_FILE.length());
        }
        return FileWrite.INSTANCE.writePrivateShellFile(fileName, fileName, context);
    }

    public static String executeResultRoot(Context context, String script, NodeInfoBase nodeInfoBase) {
        return executeResultRoot(context, script, nodeInfoBase, null);
    }

    // Overload cho phép truyền thêm các biến môi trường tuỳ chỉnh (ví dụ: state, menu_id)
    // - dùng khi cần chạy 1 lệnh/menu item với ngữ cảnh riêng mà không cần hiển thị dialog log.
    public static String executeResultRoot(Context context, String script, NodeInfoBase nodeInfoBase, HashMap<String, String> extraParams) {
        if (!inited) {
            init(context);
        }

        if (script == null || script.isEmpty()) {
            return "";
        }

        String script2 = script.trim();
        String path;
        if (script2.startsWith(ASSETS_FILE)) {
            path = extractScript(context, script2);
        } else {
            path = createShellCache(context, script);
        }

        if (!inited) {
            init(context);
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        if (nodeInfoBase != null && !nodeInfoBase.getCurrentPageConfigPath().isEmpty()) {
            String parentPageConfigDir = nodeInfoBase.getPageConfigDir();
            String currentPageConfigPath = nodeInfoBase.getCurrentPageConfigPath();
            stringBuilder.append("export PAGE_CONFIG_DIR='").append(parentPageConfigDir).append("'\n");
            stringBuilder.append("export PAGE_CONFIG_FILE='").append(currentPageConfigPath).append("'\n");

            if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                stringBuilder.append("export PAGE_WORK_DIR='").append(new ExtractAssets(context).getExtractPath(parentPageConfigDir)).append("'\n");
                stringBuilder.append("export PAGE_WORK_FILE='").append(new ExtractAssets(context).getExtractPath(currentPageConfigPath)).append("'\n");
            } else {
                stringBuilder.append("export PAGE_WORK_DIR='").append(parentPageConfigDir).append("'\n");
                stringBuilder.append("export PAGE_WORK_FILE='").append(currentPageConfigPath).append("'\n");
            }
        }

        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                String value = entry.getValue() == null ? "" : entry.getValue().replace("'", "'\\''");
                stringBuilder.append("export ").append(entry.getKey()).append("='").append(value).append("'\n");
            }
        }

        stringBuilder.append("\n\n");
        stringBuilder.append(environmentPath + " \"" + path + "\"");
        if (shellTranslation != null) {
            return shellTranslation.resolveRow(
                privateShell.doCmdSync(stringBuilder.toString())
            );
        } else {
            return privateShell.doCmdSync(stringBuilder.toString());
        }
    }

    private static String getStartPath(Context context) {
        String dir = FileWrite.INSTANCE.getPrivateFileDir(context);
        if (dir.endsWith("/")) {
            return dir.substring(0, dir.length() - 1);
        }
        return dir;
    }

    private static HashMap<String, String> getEnvironment(Context context) {
        HashMap<String, String> params = new HashMap<>();

        params.put("TOOLKIT", TOOKIT_DIR);
        params.put("START_DIR", getStartPath(context));
        params.put("TEMP_DIR", context.getCacheDir().getAbsolutePath());
        params.put("LANGUAGE", Locale.getDefault().getLanguage());
        params.put("COUNTRY", Locale.getDefault().getCountry());
        params.put("TIMEZONE", TimeZone.getDefault().getID());
        params.put("ANDROID_RELEASE", Build.VERSION.RELEASE);
        params.put("ANDROID_DEVICE", Build.DEVICE);
        params.put("ANDROID_BRAND", Build.BRAND);
        params.put("ANDROID_MANUFACTURER", Build.MANUFACTURER);
        params.put("ANDROID_FINGERPRINT", Build.FINGERPRINT);
        params.put("ANDROID_MODEL", Build.MODEL);
        params.put("ANDROID_ID", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        params.put("CPU_ABI", Build.CPU_ABI);
        params.put("ANDROID_SDK", String.valueOf(Build.VERSION.SDK_INT));
        params.put("KERNEL_VERSION", System.getProperty("os.version"));

        FileOwner fileOwner = new FileOwner(context);
        int androidUid = fileOwner.getUserId();
        params.put("ANDROID_UID", String.valueOf(androidUid));

        try {
            params.put("APP_USER_ID", fileOwner.getFileOwner());
        } catch (Exception ignored) {
        }

        try {
        params.put("DARK_MODE", ThemeModeState.INSTANCE.isDarkMode() ? "true" : "false");
        } catch (Exception ignored) {
        }

        params.put("ROOT_PERMISSION", rooted ? "true" : "false");
        params.put("SDCARD_PATH", Environment.getExternalStorageDirectory().getAbsolutePath());

        try {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        params.put("PACKAGE_NAME", context.getPackageName());
        params.put("PACKAGE_VERSION_NAME", packageInfo.versionName);
        params.put("PATH_APK", context.getApplicationInfo().sourceDir);
        params.put("APP_UID", String.valueOf(android.os.Process.myUid()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                params.put("PACKAGE_VERSION_CODE", String.valueOf(packageInfo.getLongVersionCode()));
            } else {
                params.put("PACKAGE_VERSION_CODE", String.valueOf(packageInfo.versionCode));
            }
        } catch (Exception ex) {}

        return params;
    }

    private static ArrayList<String> getVariables(HashMap<String, String> params) {
        ArrayList<String> envp = new ArrayList<>();

        if (params != null) {
            for (String key : params.keySet()) {
                String value = params.get(key);
                if (value == null) {
                    value = "";
                }
                envp.add(key + "='" + value.replaceAll("'", "'\\\\''") + "'");
            }
        }

        return envp;
    }

    private static String getExecuteScript(Context context, String script, String tag) {
        if (!inited) {
            init(context);
        }

        if (script == null || script.isEmpty()) {
            return "";
        }

        String script2 = script.trim();
        String cachePath;
        if (script2.startsWith(ASSETS_FILE)) {
            cachePath = extractScript(context, script2);
            if (cachePath == null) {
                cachePath = script;
            }
        } else {
            cachePath = createShellCache(context, script);
        }


        return environmentPath + " \"" + cachePath + "\" \"" + tag +  "\"";
    }

    static Process getRuntime() {
        try {
            if (rooted) {
                try {
                    return Runtime.getRuntime().exec("su");
                } catch (Exception ignored) {
                    return Runtime.getRuntime().exec("sh");
                }
            } else {
                return Runtime.getRuntime().exec("sh");
            }
        } catch (Exception ex) {
            return null;
        }
    }

    public static void executeShell(
            Context context,
            DataOutputStream dataOutputStream,
            String cmds,
            HashMap<String, String> params,
            NodeInfoBase nodeInfo,
            String tag) {
        executeShell(context, dataOutputStream, cmds, params, nodeInfo, tag, false);
    }

    public static void executeShell(
            Context context,
            DataOutputStream dataOutputStream,
            String cmds,
            HashMap<String, String> params,
            NodeInfoBase nodeInfo,
            String tag,
            boolean needInput) {

        if (params == null) {
            params = new HashMap<>();
        }

        if (nodeInfo != null) {
            String parentPageConfigDir = nodeInfo.getPageConfigDir();
            String currentPageConfigPath = nodeInfo.getCurrentPageConfigPath();
            if (parentPageConfigDir != null && !parentPageConfigDir.isEmpty()) {
                params.put("PAGE_CONFIG_DIR", parentPageConfigDir);
            }
            if (currentPageConfigPath != null && !currentPageConfigPath.isEmpty()) {
                params.put("PAGE_CONFIG_FILE", currentPageConfigPath);
                if (currentPageConfigPath.startsWith("file:///android_asset/")) {
                    String workDir = new ExtractAssets(context).getExtractPath(parentPageConfigDir);
                    String workFile = new ExtractAssets(context).getExtractPath(currentPageConfigPath);
                    if (workDir != null && !workDir.isEmpty()) {
                        params.put("PAGE_WORK_DIR", workDir);
                    }
                    if (workFile != null && !workFile.isEmpty()) {
                        params.put("PAGE_WORK_FILE", workFile);
                    }
                } else {
                    params.put("PAGE_WORK_DIR", parentPageConfigDir);
                    params.put("PAGE_WORK_FILE", currentPageConfigPath);
                }
            }
        }

        ArrayList<String> envp = getVariables(params);
        StringBuilder envpCmds = new StringBuilder();
        if (!envp.isEmpty()) {
            for (String param : envp) {
                envpCmds.append("export ").append(param).append("\n");
            }
        }
        try {
            dataOutputStream.write(envpCmds.toString().getBytes(StandardCharsets.UTF_8));

            String executeScript = getExecuteScript(context, cmds, tag);
            if (executeScript == null || executeScript.isEmpty()) {
                return;
            }
            if (needInput) {
                dataOutputStream.write((executeScript + "; sleep 0.2; exit\n").getBytes(StandardCharsets.UTF_8));
            } else {
                dataOutputStream.write((executeScript + "\n\nsleep 0.2; exit\nexit\n").getBytes(StandardCharsets.UTF_8));
            }
            dataOutputStream.flush();
        } catch (Exception ignored) {
        }
    }
}