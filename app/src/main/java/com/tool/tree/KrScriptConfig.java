package com.tool.tree;

import android.content.Context;

import com.omarea.krscript.executor.ScriptEnvironmen;
import com.omarea.krscript.model.PageNode;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

public class KrScriptConfig {
    private static final String ASSETS_FILE = "file:///android_asset/";

    private final static String TOOLKIT_DIR = "toolkit_dir";
    private final static String TOOLKIT_DIR_DEFAULT = "file:///android_asset/home";

    private final static String EXECUTOR_CORE = "executor_core";
    private final static String PAGE_LIST_CONFIG = "page_list_config";
    private final static String PAGE_LIST_CONFIG_SH = "page_list_config_sh";
    private final static String FAVORITE_CONFIG = "favorite_config";
    private final static String FAVORITE_CONFIG_SH = "favorite_config_sh";
    private final static String CUSTOM_TAB3_CONFIG = "custom_tab3_config";
    private final static String CUSTOM_TAB3_CONFIG_SH = "custom_tab3_config_sh";
    private final static String CUSTOM_TAB4_CONFIG = "custom_tab4_config";
    private final static String CUSTOM_TAB4_CONFIG_SH = "custom_tab4_config_sh";
    private final static String BEFORE_START_SH = "before_start_sh";

    private static HashMap<String, String> configInfo;

    private final String EXECUTOR_CORE_DEFAULT = "file:///android_asset/root/executor.sh";
    private final String PAGE_LIST_CONFIG_DEFAULT = "file:///android_asset/root/more.xml";
    private final String FAVORITE_CONFIG_DEFAULT = "file:///android_asset/root/favorites.xml";
    private final String CUSTOM_TAB3_DEFAULT = "file:///android_asset/root/tab3.xml";
    private final String CUSTOM_TAB4_DEFAULT = "file:///android_asset/root/tab4.xml";
    private final String BEFORE_START_SH_DEFAULT = "";

    public KrScriptConfig init(Context context) {
        if (configInfo == null) {
            configInfo = new HashMap<>();
            configInfo.put(EXECUTOR_CORE, EXECUTOR_CORE_DEFAULT);
            configInfo.put(PAGE_LIST_CONFIG, PAGE_LIST_CONFIG_DEFAULT);
            configInfo.put(FAVORITE_CONFIG, FAVORITE_CONFIG_DEFAULT);
            configInfo.put(CUSTOM_TAB3_CONFIG, CUSTOM_TAB3_DEFAULT);
            configInfo.put(CUSTOM_TAB4_CONFIG, CUSTOM_TAB4_DEFAULT);
            configInfo.put(TOOLKIT_DIR, TOOLKIT_DIR_DEFAULT);
            configInfo.put(BEFORE_START_SH, BEFORE_START_SH_DEFAULT);

            try {
                String fileName = context.getString(R.string.kr_script_config);
                if (fileName.startsWith(ASSETS_FILE)) {
                    fileName = fileName.substring(ASSETS_FILE.length());
                }
                InputStream inputStream = context.getAssets().open(fileName);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                String[] rows = new String(bytes, Charset.defaultCharset()).split("\n");
                for (String row : rows) {
                    String rowText = row.trim();
                    if (!rowText.startsWith("#") && rowText.contains("=")) {
                        int separator = rowText.indexOf("=");
                        String key = rowText.substring(0, separator).trim();
                        String value = rowText.substring(separator + 2, rowText.length() - 1).trim();
                        configInfo.remove(key);
                        configInfo.put(key, value);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            ScriptEnvironmen.init(context, getExecutorCore(), getToolkitDir());
        }

        return this;
    }

    public HashMap<String, String> getVariables() {
        return configInfo;
    }

    private String getExecutorCore() {
        if (configInfo != null && configInfo.containsKey(EXECUTOR_CORE)) {
            return configInfo.get(EXECUTOR_CORE);
        }
        return EXECUTOR_CORE_DEFAULT;
    }

    private String getToolkitDir() {
        if (configInfo != null && configInfo.containsKey(TOOLKIT_DIR)) {
            return configInfo.get(TOOLKIT_DIR);
        }
        return TOOLKIT_DIR_DEFAULT;
    }

    public PageNode getPageListConfig() {
        if (configInfo != null) {
            PageNode pageInfo = new PageNode("");
            if (configInfo.containsKey(PAGE_LIST_CONFIG_SH)) {
                pageInfo.setPageConfigSh(configInfo.get(PAGE_LIST_CONFIG_SH));
            }
            if (configInfo.containsKey(PAGE_LIST_CONFIG)) {
                pageInfo.setPageConfigPath(configInfo.get(PAGE_LIST_CONFIG));
            }
            return pageInfo;
        }
        return null;
    }

    public PageNode getFavoriteConfig() {
        if (configInfo != null) {
            PageNode pageInfo = new PageNode("");
            if (configInfo.containsKey(FAVORITE_CONFIG_SH)) {
                pageInfo.setPageConfigSh(configInfo.get(FAVORITE_CONFIG_SH));
            }
            if (configInfo.containsKey(FAVORITE_CONFIG)) {
                pageInfo.setPageConfigPath(configInfo.get(FAVORITE_CONFIG));
            }
            return pageInfo;
        }
        return null;
    }

    public PageNode getCustomTab3Config() {
        if (configInfo != null) {
            PageNode pageInfo = new PageNode("");
            if (configInfo.containsKey("custom_tab3_config_sh")) {
                pageInfo.setPageConfigSh(configInfo.get("custom_tab3_config_sh"));
            }
            if (configInfo.containsKey(CUSTOM_TAB3_CONFIG)) {
                pageInfo.setPageConfigPath(configInfo.get(CUSTOM_TAB3_CONFIG));
            }
            return pageInfo;
        }
        return null;
    }
    
    public PageNode getCustomTab4Config() {
        if (configInfo != null) {
            PageNode pageInfo = new PageNode("");
            if (configInfo.containsKey("custom_tab4_config_sh")) {
                pageInfo.setPageConfigSh(configInfo.get("custom_tab4_config_sh"));
            }
            if (configInfo.containsKey(CUSTOM_TAB4_CONFIG)) {
                pageInfo.setPageConfigPath(configInfo.get(CUSTOM_TAB4_CONFIG));
            }
            return pageInfo;
        }
        return null;
    }

    public String getBeforeStartSh() {
        if (configInfo != null && configInfo.containsKey(BEFORE_START_SH)) {
            return configInfo.get(BEFORE_START_SH);
        }
        return BEFORE_START_SH_DEFAULT;
    }
}