package com.omarea.common.shell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShellExecutor {
    private static String extraEnvPath = "";
    private static String defaultEnvPath = ""; // /sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin

    // Thư mục tạm mà app CHẮC CHẮN có quyền ghi (khuyến nghị: context.getCacheDir().getAbsolutePath()).
    // Dùng để ép TMPDIR trỏ vào đây thay vì giá trị mặc định của hệ thống.
    private static String extraTmpDir = "";

    public static void setExtraEnvPath(String extraEnvPath) {
        ShellExecutor.extraEnvPath = extraEnvPath;
    }

    // Gọi 1 lần lúc khởi động app, ví dụ:
    //   ShellExecutor.setTmpDir(context.getCacheDir().getAbsolutePath());
    public static void setTmpDir(String tmpDir) {
        ShellExecutor.extraTmpDir = tmpDir == null ? "" : tmpDir;
    }

    private static String getEnvPath() {
        if (extraEnvPath != null && !extraEnvPath.isEmpty()) {
            if (defaultEnvPath.isEmpty()) {
                try {
                    Process process = Runtime.getRuntime().exec("sh");
                    OutputStream outputStream = process.getOutputStream();
                    outputStream.write("echo $PATH".getBytes());
                    outputStream.flush();
                    outputStream.close();

                    InputStream inputStream = process.getInputStream();
                    byte[] cache = new byte[16384];
                    int length = inputStream.read(cache);
                    inputStream.close();
                    process.destroy();

                    String path = new String(cache, 0, length).trim();
                    if (!path.isEmpty()) {
                        defaultEnvPath = path;
                    } else {
                        throw new RuntimeException("Unable to obtain $PATH parameter");
                    }
                } catch (Exception ex) {
                    defaultEnvPath = "/system/bin:/vendor/bin:/odm/bin:/system/xbin:/vendor/xbin:/system/sbin:/sbin";
                }
            }

            String path = defaultEnvPath;

            return ( "PATH=" + path + ":" + extraEnvPath);
        }

        return null;
    }

    // FIXED (trước đây là FIXME): ở chế độ non-root, biến TMPDIR mặc định của tiến trình
    // (thường là /data/local/tmp) app KHÔNG có quyền ghi -> các script dùng lệnh `source`,
    // `mktemp` hoặc bất kỳ thao tác nào cần ghi file tạm sẽ báo lỗi "Permission denied".
    // Ép TMPDIR trỏ về thư mục cache riêng của app (do setTmpDir() cung cấp, luôn ghi được
    // dù có root hay không) để tránh lỗi này. Nếu chưa gọi setTmpDir(), giữ nguyên hành vi
    // cũ (không export TMPDIR, để hệ thống tự quyết định).
    private static String buildEnvExportScript() {
        StringBuilder script = new StringBuilder();

        String envPath = getEnvPath();
        if (envPath != null) {
            script.append("export ").append(envPath).append("\n");
        }

        if (extraTmpDir != null && !extraTmpDir.isEmpty()) {
            script.append("export TMPDIR='").append(extraTmpDir).append("'\n");
        }

        return script.length() > 0 ? script.toString() : null;
    }

    private static Process getProcess(String run) throws IOException {
        String env = buildEnvExportScript();
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(run);
        if (env != null) {
            OutputStream outputStream = process.getOutputStream();
            outputStream.write(env.getBytes());
            outputStream.flush();
        }
        return process;
    }

    public static Process getSuperUserRuntime() throws IOException {
        try {
            return getProcess("su");
        } catch (IOException e) {
            return getProcess("sh");
        }
    }

    public static Process getRuntime() throws IOException {
        return getProcess("sh");
    }
}
