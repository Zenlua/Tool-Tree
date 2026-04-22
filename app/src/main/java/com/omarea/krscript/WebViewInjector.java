package com.omarea.krscript;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.ShellExecutor;
import com.omarea.common.ui.DialogHelper;
import com.omarea.krscript.downloader.Downloader;
import com.omarea.krscript.executor.ExtractAssets;
import com.omarea.krscript.executor.ScriptEnvironmen;
import com.omarea.krscript.model.NodeInfoBase;
import com.omarea.krscript.model.ShellHandlerBase;
import com.omarea.krscript.ui.ParamsFileChooserRender;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import com.tool.tree.R;

public class WebViewInjector {
    private final WebView webView;
    private final Context context;
    private final ParamsFileChooserRender.FileChooserInterface fileChooser;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    public WebViewInjector(WebView webView, ParamsFileChooserRender.FileChooserInterface fileChooser) {
        this.webView = webView;
        this.context = webView.getContext();
        this.fileChooser = fileChooser;
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    public void inject(final Activity activity, final boolean credible) {
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();
            
            // --- TỐI ƯU HÓA WEBVIEW ---
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true); // Quan trọng để web chạy nhanh
            webSettings.setDatabaseEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // Sử dụng cache hệ thống
            
            // Tạm thời chặn ảnh để ưu tiên tải cấu trúc HTML và Script Shell
            webSettings.setBlockNetworkImage(true);
            
            webSettings.setAllowFileAccess(credible);
            webSettings.setAllowUniversalAccessFromFileURLs(credible);
            webSettings.setAllowFileAccessFromFileURLs(credible);
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            // ---------------------------

            webView.addJavascriptInterface(
                    new KrScriptEngine(context),
                    "KrScriptCore"
            );
            
            webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                DialogHelper.Companion.animDialog(new AlertDialog.Builder(context)
                        .setTitle(R.string.kr_download_confirm)
                        .setMessage(url + "\n\n" + mimetype + "\n" + contentLength + "Bytes")
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> 
                            new Downloader(context, null).downloadBySystem(url, contentDisposition, mimetype, UUID.randomUUID().toString(), null))
                        .setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
                        })).setCancelable(false);
            });
        }
    }

    private class KrScriptEngine {
        private final Context context;
        private final NodeInfoBase virtualRootNode = new NodeInfoBase("");

        private KrScriptEngine(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public boolean rootCheck() {
            return KeepShellPublic.INSTANCE.checkRoot();
        }

        @JavascriptInterface
        public String executeShell(String script) {
            if (script != null && !script.isEmpty()) {
                return ScriptEnvironmen.executeResultRoot(context, script, virtualRootNode);
            }
            return "";
        }

        @JavascriptInterface
        public boolean executeShellAsync(String script, String callbackFunction, String env) {
            HashMap<String, String> params = new HashMap<>();
            Process process = null;
            try {
                if (env != null && !env.isEmpty()) {
                    JSONObject paramsObject = new JSONObject(env);
                    for (Iterator<String> it = paramsObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        params.put(key, paramsObject.getString(key));
                    }
                }
                process = ShellExecutor.getSuperUserRuntime();
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (process != null) {
                final OutputStream outputStream = process.getOutputStream();
                final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                setHandler(process, callbackFunction, () -> { });

                ScriptEnvironmen.executeShell(context, dataOutputStream, script, params, null, null);
                return true;
            } else {
                return false;
            }
        }

        @JavascriptInterface
        public String extractAssets(String assets) {
            return new ExtractAssets(context).extractResource(assets);
        }

        @JavascriptInterface
        public boolean fileChooser(final String callbackFunction) {
            if (fileChooser != null) {
                return fileChooser.openFileChooser(new ParamsFileChooserRender.FileSelectedInterface() {
                    @Override
                    public int type() {
                        return ParamsFileChooserRender.FileSelectedInterface.Companion.getTYPE_FILE();
                    }

                    @Nullable
                    @Override
                    public String suffix() {
                        return null;
                    }

                    @NotNull
                    @Override
                    public String mimeType() {
                        return "*/*";
                    }

                    @Override
                    public void onFileSelected(@Nullable String path) {
                        // Trả dữ liệu về UI thread để tránh crash khi gọi JS
                        mainHandler.post(() -> {
                            try {
                                final JSONObject message = new JSONObject();
                                message.put("absPath", (path == null || path.isEmpty()) ? null : path);
                                webView.evaluateJavascript(callbackFunction + "(" + message + ")", null);
                            } catch (Exception ignored) { }
                        });
                    }
                });
            }
            return false;
        }

        private void setHandler(Process process, final String callbackFunction, final Runnable onExit) {
            final InputStream inputStream = process.getInputStream();
            final InputStream errorStream = process.getErrorStream();
            
            // Thread đọc luồng Standard Output
            final Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sendJsLog(callbackFunction, ShellHandlerBase.EVENT_REDE, line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Thread đọc luồng Error Output
            final Thread readerError = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sendJsLog(callbackFunction, ShellHandlerBase.EVENT_READ_ERROR, line + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
                    sendJsLog(callbackFunction, ShellHandlerBase.EVENT_EXIT, String.valueOf(status));

                    if (reader.isAlive()) reader.interrupt();
                    if (readerError.isAlive()) readerError.interrupt();
                    if (onExit != null) onExit.run();
                }
            });

            reader.start();
            readerError.start();
            waitExit.start();
        }

        // Hàm hỗ trợ gửi log về WebView thông qua mainHandler để ổn định hiệu suất
        private void sendJsLog(String callback, int type, String messageStr) {
            mainHandler.post(() -> {
                try {
                    final JSONObject message = new JSONObject();
                    message.put("type", type);
                    message.put("message", messageStr);
                    webView.evaluateJavascript(callback + "(" + message + ")", null);
                } catch (Exception ignored) { }
            });
        }
    }
}
