package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleShellWatcher {
    private static final int READ_CHUNK_SIZE = 1024; // Tăng kích thước chunk lên một chút để đọc nhanh hơn với log lớn
    private static final long POLL_INTERVAL_MS = 15;   // Giảm thời gian poll xuống 15ms để log hiển thị tức thời hơn

    private void readStreams(InputStream inputStream, InputStream errorStream, final ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation) {
        try (InputStreamReader isrOut = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             InputStreamReader isrErr = new InputStreamReader(errorStream, StandardCharsets.UTF_8)) {

            StringBuilder bufOut = new StringBuilder();
            StringBuilder bufErr = new StringBuilder();
            char[] chunk = new char[READ_CHUNK_SIZE];

            boolean doneOut = false;
            boolean doneErr = false;

            while (!doneOut || !doneErr) {
                boolean progressed = false;

                if (!doneOut) {
                    try {
                        while (isrOut.ready()) {
                            int n = isrOut.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneOut = true;
                                break;
                            } else {
                                processChunk(chunk, n, bufOut, ShellHandlerBase.EVENT_REDE, shellHandlerBase, shellTranslation);
                                progressed = true;
                            }
                        }
                    } catch (IOException streamClosed) {
                        doneOut = true;
                    }
                }

                if (!doneErr) {
                    try {
                        while (isrErr.ready()) {
                            int n = isrErr.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneErr = true;
                                break;
                            } else {
                                processChunk(chunk, n, bufErr, ShellHandlerBase.EVENT_READ_ERROR, shellHandlerBase, shellTranslation);
                                progressed = true;
                            }
                        }
                    } catch (IOException streamClosed) {
                        doneErr = true;
                    }
                }

                if (!progressed) {
                    flushPending(bufOut, ShellHandlerBase.EVENT_REDE, shellHandlerBase, shellTranslation, false);
                    flushPending(bufErr, ShellHandlerBase.EVENT_READ_ERROR, shellHandlerBase, shellTranslation, false);

                    if (doneOut && doneErr) break;

                    try {
                        Thread.sleep(POLL_INTERVAL_MS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            flushPending(bufOut, ShellHandlerBase.EVENT_REDE, shellHandlerBase, shellTranslation, true);
            flushPending(bufErr, ShellHandlerBase.EVENT_READ_ERROR, shellHandlerBase, shellTranslation, true);
        }
    }

    /**
     * Tối ưu hóa xử lý chunk bằng cách quét chỉ mục thay vì append từng ký tự,
     * giúp giảm đáng kể chi phí cấp phát bộ nhớ (Garbage Collection).
     */
    private void processChunk(char[] chunk, int len, StringBuilder buffer, int what, ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation) {
        int lastIdx = 0;
        for (int i = 0; i < len; i++) {
            char c = chunk[i];
            if (c == '\n' || c == '\r') {
                buffer.append(chunk, lastIdx, (i - lastIdx) + 1);
                String segment = buffer.toString();
                buffer.setLength(0);
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(what, shellTranslation.resolveRow(segment))
                );
                lastIdx = i + 1;
            }
        }
        if (lastIdx < len) {
            buffer.append(chunk, lastIdx, len - lastIdx);
        }
    }

    private void flushPending(StringBuilder buffer, int what, ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation, boolean forceTrailingNewline) {
        if (buffer.length() == 0) return;
        String text = buffer.toString();
        buffer.setLength(0);
        String resolved = shellTranslation.resolveRow(text);
        if (forceTrailingNewline && !resolved.endsWith("\n") && !resolved.endsWith("\r")) {
            resolved = resolved + "\n";
        }
        shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(what, resolved));
    }

    public void setHandler(Context context, Process process, final ShellHandlerBase shellHandlerBase, final Runnable onExit) {
        final ShellTranslation shellTranslation = new ShellTranslation(context);
        final InputStream inputStream = process.getInputStream();
        final InputStream errorStream = process.getErrorStream();

        final Thread reader = new Thread(() ->
            readStreams(inputStream, errorStream, shellHandlerBase, shellTranslation)
        , "ShellReader-Thread");

        Thread waitExit = new Thread(() -> {
            int status = -1;
            try {
                status = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(ShellHandlerBase.EVENT_EXIT, status));
                if (reader.isAlive()) {
                    reader.interrupt();
                }
                if (onExit != null) {
                    onExit.run();
                }
            }
        }, "ShellWaiter-Thread");

        reader.start();
        waitExit.start();
    }
}
