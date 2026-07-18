package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleShellWatcher {
    /**
     * Đọc 1 stream theo từng ký tự, giữ nguyên '\r' và '\n' (không dùng BufferedReader.readLine(),
     * vì readLine() coi cả '\r' đơn lẻ là dấu kết thúc dòng và sẽ xoá mất nó, khiến các dòng
     * progress dùng "\r" để ghi đè (vd: "Đang tải 10%\r") bị tách thành nhiều dòng log riêng biệt
     * và mất luôn thông tin '\r' cần thiết để UI gộp dòng lại).
     *
     * @param inputStream      stream cần đọc (stdout hoặc stderr)
     * @param shellHandlerBase handler để gửi message
     * @param what             loại message (EVENT_REDE cho stdout, EVENT_READ_ERROR cho stderr)
     * @param shellTranslation dùng để dịch/resolve từng dòng trước khi gửi lên UI
     */
    private void readStream(InputStream inputStream, final ShellHandlerBase shellHandlerBase, int what, ShellTranslation shellTranslation) {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            StringBuilder buffer = new StringBuilder();
            int ch;
            while ((ch = isr.read()) != -1) {
                char c = (char) ch;
                buffer.append(c);
                if (c == '\n' || c == '\r') {
                    String segment = buffer.toString();
                    shellHandlerBase.sendMessage(
                        shellHandlerBase.obtainMessage(what, shellTranslation.resolveRow(segment))
                    );
                    buffer.setLength(0);
                }
            }
            if (buffer.length() > 0) {
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(what, shellTranslation.resolveRow(buffer.toString()) + "\n")
                );
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 设置日志处理Handler
     *
     * @param process          Runtime进程
     * @param shellHandlerBase ShellHandlerBase
     */
    public void setHandler(Context context, Process process, final ShellHandlerBase shellHandlerBase, final Runnable onExit) {
        final ShellTranslation shellTranslation = new ShellTranslation(context);
        final InputStream inputStream = process.getInputStream();
        final InputStream errorStream = process.getErrorStream();

        final Thread reader = new Thread(() ->
            readStream(inputStream, shellHandlerBase, ShellHandlerBase.EVENT_REDE, shellTranslation)
        );

        final Thread readerError = new Thread(() ->
            readStream(errorStream, shellHandlerBase, ShellHandlerBase.EVENT_READ_ERROR, shellTranslation)
        );

        final Process processFinal = process;
        Thread waitExit = new Thread(() -> {
            int status = -1;
            try {
                status = processFinal.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_EXIT, status));
                if (reader.isAlive()) {
                    reader.interrupt();
                }
                if (readerError.isAlive()) {
                    readerError.interrupt();
                }
                if (onExit != null) {
                    onExit.run();
                }
            }
        });

        reader.start();
        readerError.start();
        waitExit.start();
    }
}
