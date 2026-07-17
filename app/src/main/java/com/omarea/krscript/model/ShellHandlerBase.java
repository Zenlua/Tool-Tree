package com.omarea.krscript.model;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hello on 2018/04/01.
 * Optimized for performance, regex flexbility, and safety without breaking the base context.
 */
public abstract class ShellHandlerBase extends Handler {
    public static final int EVENT_START = 0;
    public static final int EVENT_REDE = 2; // Giữ nguyên typo cũ của gốc để tránh break-change
    public static final int EVENT_READ_ERROR = 4;
    public static final int EVENT_WRITE = 6;
    public static final int EVENT_EXIT = -2;

    // Compile sẵn các Pattern tĩnh giúp tăng tốc độ xử lý luồng log liên tục
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\x1B\\[[0-9;]*[a-zA-Z]");
    private static final Pattern AM_PATTERN = Pattern.compile("am:\\[(.*?)\\]");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("progress:\\[(.*?)\\]");
    private static final Pattern INPUT_PATTERN = Pattern.compile("input:\\[(.*?)\\]");
    protected boolean autoNeedInput = false;
    protected abstract void onProgress(int current, int total);
    protected abstract void onStart(Object msg);
    public abstract void onStart(Runnable forceStop);
    protected abstract void onExit(Object msg);
    protected abstract void updateLog(final SpannableString msg);

    // GIỮ NGUYÊN GỐC: Context truyền thống để tránh lỗi biên dịch của các lớp con bên ngoài
    protected Context context;

    // Tham chiếu tới luồng ghi (stdin) của tiến trình shell đang chạy. Đây là tham chiếu MẠNH
    // (không dùng WeakReference) vì ShellExecutor không giữ biến này ở nơi nào khác — nếu dùng
    // weak reference, DataOutputStream sẽ có thể bị GC gần như ngay sau khi execute() trả về,
    // khiến ô nhập liệu mất tác dụng. Việc giải phóng được thực hiện chủ động qua unbindStdin()
    // (gọi từ release() khi dialog bị huỷ) để không giữ rác sau khi không cần nữa.
    private DataOutputStream stdin;

    public ShellHandlerBase(Context context) {
        this.context = context;
    }

    /**
     * Gắn luồng stdin của process shell hiện tại, để UI (ô nhập liệu) có thể ghi dữ liệu
     * người dùng gõ vào ngay trong lúc script đang chạy (phục vụ lệnh `read` trong script).
     */
    public void bindStdin(DataOutputStream stdin) {
        this.stdin = stdin;
    }

    public void unbindStdin() {
        this.stdin = null;
    }

    /**
     * Ghi một dòng văn bản do người dùng nhập vào stdin của shell (kèm ký tự xuống dòng để
     * lệnh `read` trong script coi đây là một dòng nhập hoàn chỉnh).
     * Dùng UTF-8 thay vì writeBytes() (chỉ ghi byte thấp) để hỗ trợ đúng tiếng Việt có dấu.
     */
    public boolean writeInput(String text) {
        DataOutputStream stdin = this.stdin;
        if (stdin == null || text == null) {
            return false;
        }
        try {
            stdin.write(text.getBytes(StandardCharsets.UTF_8));
            stdin.writeBytes("\n");
            stdin.flush();
            return true;
        } catch (IOException e) {
            // Stream đã đóng (script đã kết thúc / bị huỷ) -> tự huỷ tham chiếu để tránh gọi lại vô ích
            this.stdin = null;
            return false;
        }
    }

    /**
     * Được gọi khi script chủ động báo hiệu cần người dùng nhập liệu, thông qua cú pháp
     * "input:[gợi ý hiển thị]" trong output (tương tự am:[...] / progress:[...]).
     * Mặc định không làm gì; lớp con (ví dụ DialogLogFragment.MyShellHandler) override để
     * hiện ô nhập kèm gợi ý (prompt).
     */
    protected void onInputRequest(String prompt) {
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case EVENT_EXIT:
                onExit(msg.obj);
                break;
            case EVENT_START:
                onStart(msg.obj);
                break;
            case EVENT_REDE:
                onReaderMsg(msg.obj);
                break;
            case EVENT_READ_ERROR:
                onError(msg.obj);
                break;
            case EVENT_WRITE:
                onWrite(msg.obj);
                break;
        }
    }

    protected void onReaderMsg(Object msg) {
        if (msg == null) return;
        
        String log = msg.toString();
        String cleanLog = ANSI_ESCAPE_PATTERN.matcher(log).replaceAll("").trim();
    
        // === TỰ ĐỘNG PHÁT HIỆN PROMPT CẦN INPUT ===
        if (shouldShowInputPrompt(cleanLog)) {
            autoNeedInput = true;
            onInputRequest(cleanLog);
            // Gọi để DialogLogFragment hiển thị ô nhập
            return;
        }
    
        // Parser cũ giữ nguyên
        Matcher amMatcher = AM_PATTERN.matcher(cleanLog);
        if (amMatcher.find()) {
            String args = amMatcher.group(1).trim();
            if (args.equalsIgnoreCase("help")) {
                updateLog(new SpannableString(getAmHelp()));
            } else if (!args.isEmpty()) {
                onAm(args);
            }
            return;
        }
    
        Matcher inputMatcher = INPUT_PATTERN.matcher(cleanLog);
        if (inputMatcher.find()) {
            String prompt = inputMatcher.group(1).trim();
            onInputRequest(prompt);
            return;
        }
    
        Matcher progressMatcher = PROGRESS_PATTERN.matcher(cleanLog);
        if (progressMatcher.find()) {
            try {
                String content = progressMatcher.group(1).trim();
                int slashIdx = content.indexOf('/');
                if (slashIdx > 0) {
                    int start = Integer.parseInt(content.substring(0, slashIdx).trim());
                    int total = Integer.parseInt(content.substring(slashIdx + 1).trim());
                    onProgress(start, total);
                    return;
                }
            } catch (Exception e) {
                updateLog("Format error: " + cleanLog, "#ff0000");
                return;
            }
        }
        
        onReader(msg);
    }
    
    /**
     * Tự động phát hiện prompt cần người dùng nhập
     */
    private boolean shouldShowInputPrompt(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        
        String lower = line.toLowerCase(Locale.getDefault()).trim();
        return 
            lower.contains("y/n") ||
            lower.contains("yes/no");
    }

    // Thêm getter để ShellExecutor lấy được
    public boolean isAutoNeedInput() {
        return autoNeedInput;
    }

    protected void onReader(Object msg) {
        updateLog(msg, "#00cc55");
    }

    protected void onWrite(Object msg) {
        updateLog(msg, "#808080");
    }

    protected void onError(Object msg) {
        updateLog(msg, "#ff0000");
    }

    private String getAmHelp() {
        return "am:[command] syntax:\n\n" +
                "am:[start -a ACTION -d URI -n PACKAGE/CLASS]\n" +
                "am:[startservice -n PACKAGE/CLASS]\n" +
                "am:[foregroundservice -n PACKAGE/CLASS]\n" +
                "am:[broadcast -a ACTION]\n\n" +
                "Extras:\n" +
                "  --es key value    String\n" +
                "  --ei key value    Int\n" +
                "  --ez key value    Boolean\n" +
                "  --el key value    Long\n" +
                "  --ef key value    Float\n" +
                "  --ed key value    Double\n" +
                "  --eu key value    Uri\n" +
                "  --esa key v1 v2   String[]\n" +
                "  --eia key v1 v2   Int[]\n";
    }

    private void onAm(String args) {
        ArrayList<String> tokens = splitArgs(args);
        if (tokens.isEmpty()) return;

        String cmd = tokens.get(0).toLowerCase(Locale.US);
        
        try {
            Intent intent = parseIntentFromTokens(tokens);

            if (Intent.ACTION_SEND.equals(intent.getAction()) ||
                    Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {

                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri == null) uri = intent.getData();

                if (uri != null) {
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setClipData(ClipData.newRawUri(null, uri));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            switch (cmd) {
                case "start":
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if ((Intent.ACTION_SEND.equals(intent.getAction())
                            || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
                            && intent.getComponent() == null) {

                        Intent chooser = Intent.createChooser(intent, null);
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(chooser);
                    } else {
                        context.startActivity(intent);
                    }
                    break;

                case "foregroundservice":
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(intent);
                    }
                    break;

                case "startservice":
                    context.startService(intent);
                    break;

                case "broadcast":
                    context.sendBroadcast(intent);
                    break;
            }

        } catch (Exception e) {
            updateLog(e.toString(), "#ff0000");
        }
    }

    private Intent parseIntentFromTokens(ArrayList<String> tokens) {
        Intent intent = new Intent();
        int i = 1;

        while (i < tokens.size()) {
            String token = tokens.get(i);
            try {
                switch (token) {
                    case "-a":
                        if (i + 1 < tokens.size()) intent.setAction(tokens.get(++i));
                        break;

                    case "-d":
                        if (i + 1 < tokens.size()) {
                            String value = stripQuote(tokens.get(++i));
                            Uri uri;
                            if (value.contains("://"))
                                uri = Uri.parse(value);
                            else if (value.startsWith("/"))
                                uri = Uri.fromFile(new File(value));
                            else
                                uri = Uri.parse(value);
                            intent.setData(uri);
                        }
                        break;

                    case "-t":
                        if (i + 1 < tokens.size()) intent.setType(tokens.get(++i));
                        break;

                    case "-n":
                        if (i + 1 < tokens.size()) {
                            String[] cn = tokens.get(++i).split("/", 2);
                            if (cn.length == 2)
                                intent.setComponent(new ComponentName(cn[0], cn[1]));
                        }
                        break;

                    case "-p":
                        if (i + 1 < tokens.size()) intent.setPackage(tokens.get(++i));
                        break;

                    case "-c":
                        if (i + 1 < tokens.size()) intent.addCategory(tokens.get(++i));
                        break;

                    case "-f":
                        if (i + 1 < tokens.size()) {
                            String v = tokens.get(++i);
                            int flags = v.startsWith("0x") ?
                                    Integer.parseInt(v.substring(2), 16) :
                                    Integer.parseInt(v);
                            intent.addFlags(flags);
                        }
                        break;

                    case "--es":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), tokens.get(++i));
                        break;

                    case "--ei":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), Integer.parseInt(tokens.get(++i)));
                        break;

                    case "--el":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), Long.parseLong(tokens.get(++i)));
                        break;

                    case "--ez":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), Boolean.parseBoolean(tokens.get(++i)));
                        break;

                    case "--ef":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), Float.parseFloat(tokens.get(++i)));
                        break;

                    case "--ed":
                        if (i + 2 < tokens.size())
                            intent.putExtra(tokens.get(++i), Double.parseDouble(tokens.get(++i)));
                        break;

                    case "--eu":
                        if (i + 2 < tokens.size()) {
                            String key = tokens.get(++i);
                            String value = stripQuote(tokens.get(++i));
                            Uri uri = value.contains("://") ? Uri.parse(value) : Uri.fromFile(new File(value));
                            intent.putExtra(key, uri);
                        }
                        break;

                    case "--esn":
                        if (i + 1 < tokens.size())
                            intent.putExtra(tokens.get(++i), (String) null);
                        break;

                    case "--esa":
                        if (i + 1 < tokens.size()) {
                            String key = tokens.get(++i);
                            ArrayList<String> list = new ArrayList<>();
                            while (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
                                list.add(tokens.get(++i));
                            }
                            intent.putExtra(key, list.toArray(new String[0]));
                        }
                        break;

                    case "--eia":
                        if (i + 1 < tokens.size()) {
                            String key = tokens.get(++i);
                            ArrayList<Integer> list = new ArrayList<>();
                            while (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
                                list.add(Integer.parseInt(tokens.get(++i)));
                            }
                            int[] arr = new int[list.size()];
                            for (int j = 0; j < list.size(); j++) arr[j] = list.get(j);
                            intent.putExtra(key, arr);
                        }
                        break;

                    case "--grant-read-uri-permission":
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        break;

                    case "--grant-write-uri-permission":
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        break;
                }
            } catch (NumberFormatException e) {
                updateLog("Number formatting error at the parameter: " + token, "#ff0000");
            }
            i++;
        }
        return intent;
    }

    private String stripQuote(String s) {
        if (s != null && s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private ArrayList<String> splitArgs(String args) {
        ArrayList<String> out = new ArrayList<>();
        if (args == null || args.isEmpty()) return out;
        
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);

            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                } else if (c == '\\' && i + 1 < args.length()) {
                    char n = args.charAt(++i);
                    switch (n) {
                        case 'n': cur.append('\n'); break;
                        case 't': cur.append('\t'); break;
                        case '\\': cur.append('\\'); break;
                        case '"': cur.append('"'); break;
                        case '\'': cur.append('\''); break;
                        default: cur.append(n);
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true;
                    quoteChar = c;
                } else if (Character.isWhitespace(c)) {
                    if (cur.length() > 0) {
                        out.add(cur.toString());
                        cur.setLength(0);
                    }
                } else {
                    cur.append(c);
                }
            }
        }

        if (cur.length() > 0) {
            out.add(cur.toString());
        }

        return out;
    }

    protected void updateLog(final Object msg, final String color) {
        if (msg != null) {
            updateLog(msg, Color.parseColor(color));
        }
    }

    protected void updateLog(final Object msg, final int color) {
        if (msg != null) {
            String msgStr = msg.toString();
            SpannableString spannableString = new SpannableString(msgStr);
            spannableString.setSpan(new ForegroundColorSpan(color), 0, msgStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateLog(spannableString);
        }
    }
}