package com.omarea.krscript.executor;

import android.content.Context;

import com.omarea.common.shell.ShellTranslation;
import com.omarea.krscript.model.ShellHandlerBase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleShellWatcher {
    // Số ký tự tối đa đọc mỗi lần gọi read() cho 1 stream. Với những dòng dài hơn giá trị này,
    // vòng lặp bên dưới sẽ tự động gọi read() nhiều lần liên tiếp (miễn ready() còn true) để
    // đọc CẠN dữ liệu hiện có trước khi chuyển qua stream còn lại - xem giải thích trong
    // readStreams().
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
     * LƯU Ý VỀ THỨ TỰ KHI 1 DÒNG DÀI HƠN READ_CHUNK_SIZE: với 1 dòng echo dài (vd > 512 ký
     * tự), toàn bộ dữ liệu của nó thường đã nằm sẵn nguyên khối trong pipe ngay khi shell ghi
     * xong (echo ghi 1 lần, đồng bộ). Nếu mỗi vòng lặp chỉ gọi read() đúng 1 lần (tối đa
     * READ_CHUNK_SIZE ký tự) rồi lập tức chuyển qua kiểm tra stream còn lại, thì 1 dòng ngắn ở
     * stream kia (được ghi SAU nhưng đọc/gửi xong chỉ trong 1 lần vì đủ ngắn) có thể "vượt mặt"
     * và hiển thị TRƯỚC khi dòng dài kia được gửi hết lên UI - dù dòng dài được ghi trước. Vì
     * vậy, khi 1 stream vẫn còn ready() sau khi đọc, nghĩa là dữ liệu đó chắc chắn đã có sẵn từ
     * trước, ta đọc cạn nó (nhiều lần read() liên tiếp) trước khi nhường lượt cho stream kia,
     * để thứ tự message gửi lên UI khớp với thứ tự ghi thực tế của shell. Đánh đổi: nếu 1 tiến
     * trình ghi liên tục không ngừng vào 1 stream (vd vòng lặp in log tốc độ cao), stream còn
     * lại có thể phải đợi lâu hơn bình thường mới được đọc - chấp nhận được vì đây là trường
     * hợp hiếm trong các script thực tế của app.
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

                // Đọc CẠN hết dữ liệu đang sẵn có (ready()) của stdout trước khi chuyển qua
                // kiểm tra stderr, thay vì chỉ đọc 1 lần read() (tối đa READ_CHUNK_SIZE) rồi
                // lập tức nhường lượt. Lý do: nếu ready() vẫn còn true nghĩa là dữ liệu đó chắc
                // chắn đã được process ghi vào pipe từ TRƯỚC đó rồi (vd 1 dòng echo dài ghi 1
                // lần), nên phải xử lý dứt điểm nó trước khi xét tới dữ liệu ở stream kia -
                // đúng thứ tự ghi thực tế của shell. Trước đây, việc chỉ đọc 1 chunk rồi bỏ dở
                // để đi đọc stderr là nguyên nhân khiến 1 dòng lỗi ngắn (echo ... >&2, đọc/gửi
                // xong trong 1 lần vì đủ ngắn) "vượt mặt" hiển thị trước 1 dòng stdout dài hơn
                // 512 ký tự dù dòng dài được ghi trước - vì dòng dài cần NHIỀU vòng lặp mới đọc
                // hết và mới gặp '\n' để gửi message lên UI.
                if (!doneOut) {
                    try {
                        while (isrOut.ready()) {
                            int n = isrOut.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneOut = true;
                                break;
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
                        while (isrErr.ready()) {
                            int n = isrErr.read(chunk, 0, chunk.length);
                            if (n == -1) {
                                doneErr = true;
                                break;
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