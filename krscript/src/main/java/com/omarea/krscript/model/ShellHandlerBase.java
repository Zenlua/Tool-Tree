package com.omarea.krscript.model;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Pattern;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
/**
 * Created by Hello on 2018/04/01.
 */

public abstract class ShellHandlerBase extends Handler {
    /**
     * 处理启动信息
     */
    public static final int EVENT_START = 0;

    /**
     * 命令行输出内容
     */
    public static final int EVENT_REDE = 2;

    /**
     * 命令行错误输出
     */
    public static final int EVENT_READ_ERROR = 4;

    /**
     * 脚本写入日志
     */
    public static final int EVENT_WRITE = 6;

    /**
     * 处理Exitvalue
     */
    public static final int EVENT_EXIT = -2;

    protected abstract void onProgress(int current, int total);

    protected abstract void onStart(Object msg);

    public abstract void onStart(Runnable forceStop);

    protected abstract void onExit(Object msg);

    protected abstract void updateLog(final SpannableString msg);

    protected Context context;
    
    public ShellHandlerBase(Context context) {
        this.context = context;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case ShellHandlerBase.EVENT_EXIT:
                onExit(msg.obj);
                break;
            case ShellHandlerBase.EVENT_START: {
                onStart(msg.obj);
                break;
            }
            case ShellHandlerBase.EVENT_REDE:
                onReaderMsg(msg.obj);
                break;
            case ShellHandlerBase.EVENT_READ_ERROR:
                onError(msg.obj);
                break;
            case ShellHandlerBase.EVENT_WRITE: {
                onWrite(msg.obj);
                break;
            }
        }
    }

    protected void onReaderMsg(Object msg) {
        if (msg == null) return;
    
        // KHÔNG trim để giữ newline
        String log = msg.toString();
        String trimmed = log.trim();
    
        // ===== AM PARSER =====
        if (trimmed.startsWith("am:[") && trimmed.endsWith("]")) {
            String args = trimmed.substring(4, trimmed.length() - 1).trim();
    
            if (args.equalsIgnoreCase("help")) {
                updateLog(getAmHelp());
            } else if (!args.isEmpty()) {
                onAm(args);
            }
            return;
        }

        // progress:[x/y]
        if (Pattern.matches("^progress:\\[[\\-0-9]+/[0-9]+]$", log.trim())) {
            String[] values = log
                    .substring("progress:[".length(), log.indexOf("]"))
                    .split("/");
    
            int start = Integer.parseInt(values[0]);
            int total = Integer.parseInt(values[1]);
            onProgress(start, total);
            return;
        }

        onReader(msg);
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
        if (args.isEmpty()) return;
    
        ArrayList<String> tokens = splitArgs(args);
        if (tokens.isEmpty()) return;
    
        String cmd = tokens.get(0).toLowerCase(Locale.US);
        String subArgs = args.substring(cmd.length()).trim();
    
        try {
            Intent intent = parseIntentArgs(subArgs);
    
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

    private Intent parseIntentArgs(String args) {
        Intent intent = new Intent();
        ArrayList<String> tokens = splitArgs(args);
    
        int i = 0;
        while (i < tokens.size()) {
    
            String token = tokens.get(i);
    
            switch (token) {
    
                case "-a":
                    if (i + 1 < tokens.size())
                        intent.setAction(tokens.get(++i));
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
                    if (i + 1 < tokens.size())
                        intent.setType(tokens.get(++i));
                    break;
    
                case "-n":
                    if (i + 1 < tokens.size()) {
                        String[] cn = tokens.get(++i).split("/", 2);
                        if (cn.length == 2)
                            intent.setComponent(new ComponentName(cn[0], cn[1]));
                    }
                    break;
    
                case "-p":
                    if (i + 1 < tokens.size())
                        intent.setPackage(tokens.get(++i));
                    break;
    
                case "-c":
                    if (i + 1 < tokens.size())
                        intent.addCategory(tokens.get(++i));
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
                        intent.putExtra(tokens.get(++i),
                                Integer.parseInt(tokens.get(++i)));
                    break;
    
                case "--el":
                    if (i + 2 < tokens.size())
                        intent.putExtra(tokens.get(++i),
                                Long.parseLong(tokens.get(++i)));
                    break;
    
                case "--ez":
                    if (i + 2 < tokens.size())
                        intent.putExtra(tokens.get(++i),
                                Boolean.parseBoolean(tokens.get(++i)));
                    break;
    
                case "--ef":
                    if (i + 2 < tokens.size())
                        intent.putExtra(tokens.get(++i),
                                Float.parseFloat(tokens.get(++i)));
                    break;
    
                case "--ed":
                    if (i + 2 < tokens.size())
                        intent.putExtra(tokens.get(++i),
                                Double.parseDouble(tokens.get(++i)));
                    break;
    
                case "--eu":
                    if (i + 2 < tokens.size()) {
                        String key = tokens.get(++i);
                        String value = stripQuote(tokens.get(++i));
                        Uri uri = value.contains("://") ?
                                Uri.parse(value) :
                                Uri.fromFile(new File(value));
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
                        while (i + 1 < tokens.size() &&
                                !tokens.get(i + 1).startsWith("-")) {
                            list.add(tokens.get(++i));
                        }
                        intent.putExtra(key, list.toArray(new String[0]));
                    }
                    break;
    
                case "--eia":
                    if (i + 1 < tokens.size()) {
                        String key = tokens.get(++i);
                        ArrayList<Integer> list = new ArrayList<>();
                        while (i + 1 < tokens.size() &&
                                !tokens.get(i + 1).startsWith("-")) {
                            list.add(Integer.parseInt(tokens.get(++i)));
                        }
                        int[] arr = new int[list.size()];
                        for (int j = 0; j < list.size(); j++)
                            arr[j] = list.get(j);
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
            i++;
        }
        return intent;
    }

private String stripQuote(String s) {
    if (s.length() >= 2) {
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if ((first == '"' && last == '"') ||
                (first == '\'' && last == '\'')) {
            return s.substring(1, s.length() - 1);
        }
    }
    return s;
}

    private ArrayList<String> splitArgs(String args) {
        ArrayList<String> out = new ArrayList<>();
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
    
        if (cur.length() > 0)
            out.add(cur.toString());
    
        return out;
    }

    protected void updateLog(final Object msg, final String color) {
        if (msg != null) {
            String msgStr = msg.toString();
            SpannableString spannableString = new SpannableString(msgStr);
            spannableString.setSpan(new ForegroundColorSpan(Color.parseColor(color)), 0, msgStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            updateLog(spannableString);
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
