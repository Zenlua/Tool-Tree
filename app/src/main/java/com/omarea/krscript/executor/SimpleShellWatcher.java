package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleShellWatcher {

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
        final Thread reader = new Thread(() -> {
            try {
                InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                StringBuilder buffer = new StringBuilder();
                int ch;
                // Đọc từng ký tự thay vì dùng BufferedReader.readLine(), vì readLine()
                // coi cả '\r' đơn lẻ là dấu kết thúc dòng và sẽ xoá mất nó, khiến các dòng
                // progress dùng "\r" để ghi đè (vd: "\rĐang tải 10%") bị tách thành nhiều
                // dòng log riêng biệt và mất luôn thông tin \r để UI gộp dòng.
                while ((ch = isr.read()) != -1) {
                    char c = (char) ch;
                    buffer.append(c);
                    if (c == '\n' || c == '\r') {
                        String segment = buffer.toString();
                        shellHandlerBase.sendMessage(
                            shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_REDE, shellTranslation.resolveRow(segment))
                        );
                        buffer.setLength(0);
                    }
                }
                if (buffer.length() > 0) {
                    shellHandlerBase.sendMessage(
                        shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_REDE, shellTranslation.resolveRow(buffer.toString()) + "\n")
                    );
                }
            } catch (Exception ignored) {
            }
        });
        final Thread readerError = new Thread(() -> {
            String line;
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                while ((line = bufferedReader.readLine()) != null) {
                    shellHandlerBase.sendMessage(
                        shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_READ_ERROR, shellTranslation.resolveRow(line) + "\n")
                    );
                }
            } catch (Exception ignored) {
            }
        });
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