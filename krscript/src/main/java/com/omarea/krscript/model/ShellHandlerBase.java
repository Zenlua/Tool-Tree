package com.omarea.krscript.model;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;

/**
 * Created by Hello on 2018/04/01.
 */

public abstract class ShellHandlerBase extends Handler {

    protected Context context;
    
    protected Context getContext() {
        return context;
    }

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
        // String log = msg.toString();
        String log = msg.toString().replaceAll("\u001B\\[[0-?]*[ -/]*[@-~]", "");
    
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

        if (log.startsWith("am:[")) {
            int end = log.lastIndexOf(']');
            if (end > "am:[".length()) {
                String args = log.substring("am:[".length(), end).trim();
                onAm(args);
            }
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

    protected void onAm(String args) {
        Context ctx = getContext();
        if (ctx == null || args.isEmpty()) return;
        String[] tokens = splitArgs(args);
        if (tokens.length == 0) return;
        String cmd = tokens[0];
        String subArgs = args.substring(cmd.length()).trim();
    
        try {
            Intent intent = parseIntentArgs(subArgs);
    
            switch (cmd) {
                case "start": {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    
                    if ((Intent.ACTION_SEND.equals(intent.getAction())
                            || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
                            && intent.getComponent() == null) {
                        Intent chooser = Intent.createChooser(intent, null);
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.startActivity(chooser);
                    } else {
                        ctx.startActivity(intent);
                    }
                    break;
                }
    
                case "startservice":
                    ctx.startService(intent);
                    break;
    
                case "broadcast":
                    ctx.sendBroadcast(intent);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Intent parseIntentArgs(String args) {
        Intent intent = new Intent();
        String[] tokens = splitArgs(args);
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
    
                case "-a": // action
                    if (++i < tokens.length)
                        intent.setAction(tokens[i]);
                    break;
    
                case "-d": // data
                    if (++i < tokens.length)
                        intent.setData(Uri.parse(tokens[i]));
                    break;
    
                case "-t": // mime
                    if (++i < tokens.length)
                        intent.setType(tokens[i]);
                    break;
    
                case "-n": { // component
                    if (++i < tokens.length) {
                        String[] cn = tokens[i].split("/", 2);
                        if (cn.length == 2)
                            intent.setComponent(new ComponentName(cn[0], cn[1]));
                    }
                    break;
                }
    
                case "-p": // package
                    if (++i < tokens.length)
                        intent.setPackage(tokens[i]);
                    break;
    
                case "-c": // category
                    if (++i < tokens.length)
                        intent.addCategory(tokens[i]);
                    break;
    
                case "-f": { // flags
                    if (++i < tokens.length) {
                        String v = tokens[i];
                        int flags = v.startsWith("0x")
                                ? Integer.parseInt(v.substring(2), 16)
                                : Integer.parseInt(v);
                        intent.addFlags(flags);
                    }
                    break;
                }
    
                case "--es": // string
                    if (i + 2 < tokens.length)
                        intent.putExtra(tokens[++i], tokens[++i]);
                    break;
    
                case "--ei": // int
                    if (i + 2 < tokens.length) {
                        String k = tokens[++i];
                        try {
                            intent.putExtra(k, Integer.parseInt(tokens[++i]));
                        } catch (Exception ignored) {}
                    }
                    break;
    
                case "--el": // long
                    if (i + 2 < tokens.length) {
                        String k = tokens[++i];
                        try {
                            intent.putExtra(k, Long.parseLong(tokens[++i]));
                        } catch (Exception ignored) {}
                    }
                    break;
    
                case "--ez": // boolean
                    if (i + 2 < tokens.length)
                        intent.putExtra(tokens[++i], Boolean.parseBoolean(tokens[++i]));
                    break;
    
                case "--ef": // float
                    if (i + 2 < tokens.length) {
                        String k = tokens[++i];
                        try {
                            intent.putExtra(k, Float.parseFloat(tokens[++i]));
                        } catch (Exception ignored) {}
                    }
                    break;
    
                case "--ed": // double
                    if (i + 2 < tokens.length) {
                        String k = tokens[++i];
                        try {
                            intent.putExtra(k, Double.parseDouble(tokens[++i]));
                        } catch (Exception ignored) {}
                    }
                    break;
    
                case "--esn": // null
                    if (++i < tokens.length)
                        intent.putExtra(tokens[i], (String) null);
                    break;
    
                case "--esa": { // String[]
                    if (++i < tokens.length) {
                        String key = tokens[i];
                        ArrayList<String> list = new ArrayList<>();
                        while (i + 1 < tokens.length && !tokens[i + 1].startsWith("-"))
                            list.add(tokens[++i]);
                        intent.putExtra(key, list.toArray(new String[0]));
                    }
                    break;
                }
    
                case "--eia": { // int[]
                    if (++i < tokens.length) {
                        String key = tokens[i];
                        ArrayList<Integer> list = new ArrayList<>();
                        while (i + 1 < tokens.length && !tokens[i + 1].startsWith("-")) {
                            try {
                                list.add(Integer.parseInt(tokens[++i]));
                            } catch (Exception ignored) {}
                        }
                        int[] arr = new int[list.size()];
                        for (int j = 0; j < list.size(); j++) arr[j] = list.get(j);
                        intent.putExtra(key, arr);
                    }
                    break;
                }
    
                case "--grant-read-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    break;
    
                case "--grant-write-uri-permission":
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    break;
            }
        }
        return intent;
    }

    private static String[] splitArgs(String args) {
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
    
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out.toArray(new String[0]);
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
