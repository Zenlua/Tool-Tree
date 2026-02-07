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

    protected abstract void onToast(String text);

    protected abstract void onProgress(int current, int total);

    protected abstract void onStart(Object msg);

    public abstract void onStart(Runnable forceStop);

    protected abstract void onExit(Object msg);

    /**
     * 输出格式化内容
     *
     * @param msg
     */
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
        String log = msg.toString();
    
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
    
        // toast:[text...]
        if (log.matches("^toast:\\[.*]$")) {
            int end = log.lastIndexOf(']');
            if (end > "toast:[".length()) {
                String text = log.substring("toast:[".length(), end)
                        .replace("\\n", "\n");
                onToast(text);
            }
            return;
        }
        
        if (log.matches("^am:\\[(start|broadcast|service)\\|.+]$")) {
            String prefix = "am:[";
            int end = log.lastIndexOf("]");
            if (end > prefix.length()) {
                String body = log.substring(prefix.length(), end);
                int sep = body.indexOf("|");
                if (sep > 0) {
                    String type = body.substring(0, sep).trim();
                    String args = body.substring(sep + 1).trim();
                    onAm(type, args);
                }
            }
            return;
        }
    
        // log thường
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

    protected void onAm(String type, String args) {
        try {
            // Lấy Application Context nội bộ (KHÔNG cần truyền)
            Context context = android.app.ActivityThread
                    .currentApplication()
                    .getApplicationContext();
    
            if (context == null) return;
    
            Intent intent = parseIntentArgs(args);
    
            switch (type) {
                case "start":
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
    
                case "broadcast":
                    context.sendBroadcast(intent);
                    break;
    
                case "service":
                    if (Build.VERSION.SDK_INT >= 26) {
                        context.startForegroundService(intent);
                    } else {
                        context.startService(intent);
                    }
                    break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Intent parseIntentArgs(String args) {
        Intent intent = new Intent();
    
        String[] tokens = args.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
    
                case "-a": // action
                    if (i + 1 >= tokens.length) break;
                    intent.setAction(tokens[++i]);
                    break;
    
                case "-d": // data
                    if (i + 1 >= tokens.length) break;
                    intent.setData(Uri.parse(tokens[++i]));
                    break;
    
                case "-n": { // component
                    if (i + 1 >= tokens.length) break;
                    String[] cn = tokens[++i].split("/", 2);
                    if (cn.length == 2) {
                        intent.setComponent(new ComponentName(cn[0], cn[1]));
                    }
                    break;
                }
    
                case "--es": { // string extra
                    if (i + 2 >= tokens.length) break;
                    String key = tokens[++i];
                    String val = tokens[++i];
                    intent.putExtra(key, val);
                    break;
                }
    
                case "--ei": { // int extra
                    if (i + 2 >= tokens.length) break;
                    String key = tokens[++i];
                    try {
                        int val = Integer.parseInt(tokens[++i]);
                        intent.putExtra(key, val);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
    
                case "--el": { // long extra
                    if (i + 2 >= tokens.length) break;
                    String key = tokens[++i];
                    try {
                        long val = Long.parseLong(tokens[++i]);
                        intent.putExtra(key, val);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
    
                case "--ez": { // boolean extra
                    if (i + 2 >= tokens.length) break;
                    String key = tokens[++i];
                    boolean val = Boolean.parseBoolean(tokens[++i]);
                    intent.putExtra(key, val);
                    break;
                }
            }
        }
        return intent;
    }

    /**
     * 输出指定颜色的内容
     *
     * @param msg
     * @param color
     */
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
