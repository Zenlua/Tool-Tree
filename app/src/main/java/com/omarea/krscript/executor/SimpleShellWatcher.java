package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleShellWatcher {
    // Số ký tự tối đa đọc mỗi lần cho 1 stream trong 1 vòng lặp, để không "nuốt" hết dữ liệu
    // đang chờ của 1 stream (vd stdout) trước khi kiểm tra stream còn lại (stderr), giúp giữ
    // đúng thứ tự xen kẽ giữa 2 luồng.
    private static final int READ_CHUNK_SIZE = 512;
    // Khi cả 2 stream đều chưa có dữ liệu sẵn để đọc ngay, nghỉ 1 khoảng ngắn trước khi kiểm
    // tra lại, tránh busy-loop ngốn CPU. Đây cũng chính là chu kỳ "xả" (flush) phần output
    // chưa có '\r'/'\n', để các dòng kiểu progress không xuống dòng vẫn hiển thị gần như
    // tức thời thay vì phải đợi terminator kế tiếp.
    private static final long POLL_INTERVAL_MS = 40;

    /**
     * Đọc ĐỒNG THỜI cả stdout lẫn stderr trên CÙNG 1 thread, luân phiên kiểm tra từng stream
     * qua Reader.ready() (stream nào có sẵn dữ liệu thì đọc ngay, không cần đợi).
     *
     * Lý do không dùng 2 thread riêng (1 cho stdout, 1 cho stderr) như trước: stdout và stderr
     * là 2 pipe độc lập ở tầng OS, nếu đọc bằng 2 thread riêng biệt thì thứ tự message nào đến
     * UI trước hoàn toàn phụ thuộc vào việc hệ điều hành lập lịch CPU cho 2 thread đó ra sao -
     * ngẫu nhiên, không đảm bảo. Đó là lý do vì sao trước đây có lúc `echo abc >&2` (ghi trước)
     * lại hiện SAU `echo xyz` (ghi sau) trên UI. Gộp về 1 thread duy nhất, đọc luân phiên theo
     * đúng thứ tự dữ liệu thực sự sẵn sàng, giúp thứ tự hiển thị khớp với thứ tự ghi thực tế.
     *
     * Vẫn giữ nguyên cách đọc từng ký tự và giữ nguyên '\r'/'\n' (không dùng readLine()) để
     * không phá vỡ cơ chế ghi đè dòng progress dùng "\r" đã fix trước đó.
     *
     * @param inputStream      stdout của process
     * @param errorStream      stderr của process
     * @param shellHandlerBase handler để gửi message
     * @param shellTranslation dùng để dịch/resolve từng dòng trước khi gửi lên UI
     */
    private void readStreams(InputStream inputStream, InputStream errorStream, final ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation) {
        InputStreamReader isrOut = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        InputStreamReader isrErr = new InputStreamReader(errorStream, StandardCharsets.UTF_8);

        StringBuilder bufOut = new StringBuilder();
        StringBuilder bufErr = new StringBuilder();
        char[] chunk = new char[READ_CHUNK_SIZE];

        boolean doneOut = false;
        boolean doneErr = false;

        try {
            while (!doneOut || !doneErr) {
                boolean progressed = false;

                if (!doneOut) {
                    try {
                        if (isrOut.ready()) {
                            int n = isrOut.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneOut = true;
                            } else {
                                appendAndFlushTerminated(chunk, n, bufOut, ShellHandlerBase.EVENT_REDE, shellHandlerBase, shellTranslation);
                                progressed = true;
                            }
                        }
                    } catch (IOException streamClosed) {
                        doneOut = true;
                    }
                }

                if (!doneErr) {
                    try {
                        if (isrErr.ready()) {
                            int n = isrErr.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneErr = true;
                            } else {
                                appendAndFlushTerminated(chunk, n, bufErr, ShellHandlerBase.EVENT_READ_ERROR, shellHandlerBase, shellTranslation);
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
                        break;
                    }
                }
            }
        } finally {
            // Xả nốt phần dữ liệu dở dang còn lại (nếu process kết thúc giữa chừng 1 dòng)
            flushPending(bufOut, ShellHandlerBase.EVENT_REDE, shellHandlerBase, shellTranslation, true);
            flushPending(bufErr, ShellHandlerBase.EVENT_READ_ERROR, shellHandlerBase, shellTranslation, true);
        }
    }

    private void appendAndFlushTerminated(char[] chunk, int len, StringBuilder buffer, int what, ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation) {
        for (int i = 0; i < len; i++) {
            char c = chunk[i];
            buffer.append(c);
            if (c == '\n' || c == '\r') {
                String segment = buffer.toString();
                buffer.setLength(0);
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(what, shellTranslation.resolveRow(segment))
                );
            }
        }
    }

    private void flushPending(StringBuilder buffer, int what, ShellHandlerBase shellHandlerBase, ShellTranslation shellTranslation, boolean forceTrailingNewline) {
        if (buffer.length() == 0) return;
        String text = buffer.toString();
        buffer.setLength(0);
        String resolved = shellTranslation.resolveRow(text);
        if (forceTrailingNewline) {
            resolved = resolved + "\n";
        }
        shellHandlerBase.sendMessage(shellHandlerBase.obtainMessage(what, resolved));
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
            readStreams(inputStream, errorStream, shellHandlerBase, shellTranslation)
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
                if (onExit != null) {
                    onExit.run();
                }
            }
        });

        reader.start();
        waitExit.start();
    }
}
