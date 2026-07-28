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
            // Trạng thái nhận diện escape sequence đang đọc dở, để có thể flush ngay khi 1 escape
            // sequence (vd ESC[2J của lệnh `clear`) vừa đọc xong, thay vì phải đợi tới '\n'/'\r'
            // tiếp theo mới gửi đi. Rất nhiều escape sequence (clear màn hình, di chuyển con trỏ...)
            // KHÔNG kèm theo newline, nên nếu chỉ flush theo '\n'/'\r' như trước đây, dữ liệu clear
            // sẽ bị kẹt lại trong buffer của thread đọc này và không bao giờ tới được UI cho tới khi
            // có dòng output tiếp theo (hoặc tiến trình kết thúc) -> lệnh `clear` trông như "không xoá"
            // hoặc xoá bị trễ/xoá nhầm nội dung không liên quan.
            // 0 = bình thường, 1 = vừa gặp ESC (chờ ký tự kế tiếp để biết loại), 2 = trong chuỗi CSI
            // (ESC[...), chờ finalByte, 3 = trong chuỗi OSC (ESC]...), chờ BEL hoặc ESC\, 4 = trong
            // OSC vừa gặp ESC, chờ '\' để xác nhận kết thúc (ST).
            int escState = 0;
            while ((ch = isr.read()) != -1) {
                char c = (char) ch;
                buffer.append(c);
                boolean flush = false;

                switch (escState) {
                    case 0:
                        if (c == '\u001B') {
                            escState = 1;
                        } else if (c == '\n' || c == '\r') {
                            flush = true;
                        }
                        break;
                    case 1:
                        if (c == '[') {
                            escState = 2; // CSI
                        } else if (c == ']') {
                            escState = 3; // OSC
                        } else {
                            // Escape đơn (vd ESC( ESC) ESC= ESC>...) coi như đã hoàn chỉnh ngay sau
                            // ký tự này -> flush luôn để không bị kẹt buffer.
                            escState = 0;
                            flush = true;
                        }
                        break;
                    case 2:
                        // CSI kết thúc bằng 1 finalByte trong khoảng '@'..'~' (ECMA-48), vd 'J' (xoá
                        // màn hình), 'm' (màu SGR), 'H' (di chuyển con trỏ)...
                        if (c >= '@' && c <= '~') {
                            escState = 0;
                            flush = true;
                        }
                        break;
                    case 3:
                        if (c == '\u0007') {
                            escState = 0;
                            flush = true;
                        } else if (c == '\u001B') {
                            escState = 4;
                        }
                        break;
                    case 4:
                        if (c == '\\') {
                            escState = 0;
                            flush = true;
                        } else {
                            escState = 3;
                        }
                        break;
                }

                if (flush) {
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