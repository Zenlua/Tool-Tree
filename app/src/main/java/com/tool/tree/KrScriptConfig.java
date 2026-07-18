package com.tool.tree;

import android.content.Context;

import com.omarea.krscript.executor.ScriptEnvironmen;
import com.omarea.krscript.model.PageNode;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

public class KrScriptConfig {
    private static final String ASSETS_FILE = "file:///android_asset/";

    private static final String TOOLKIT_DIR = "toolkit_dir";
    private static final String TOOLKIT_DIR_DEFAULT = "file:///android_asset/home";

    private static final String EXECUTOR_CORE = "executor_core";
    private static final String PAGE_LIST_CONFIG = "page_list_config";
    private static final String PAGE_LIST_CONFIG_SH = "page_list_config_sh";
    private static final String FAVORITE_CONFIG = "favorite_config";
    private static final String FAVORITE_CONFIG_SH = "favorite_config_sh";
    private static final String CUSTOM_TAB3_CONFIG = "custom_tab3_config";
    private static final String CUSTOM_TAB3_CONFIG_SH = "custom_tab3_config_sh";
    private static final String CUSTOM_TAB4_CONFIG = "custom_tab4_config";
    private static final String CUSTOM_TAB4_CONFIG_SH = "custom_tab4_config_sh";
    private static final String BEFORE_START_SH = "before_start_sh";

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
                inputStream.close();

                String[] rows = new String(bytes, Charset.defaultCharset()).split("\n");
                for (String row : rows) {
                    String rowText = row.trim();
                    if (!rowText.startsWith("#") && rowText.contains("=")) {
                        int separator = rowText.indexOf('=');
                        String key = rowText.substring(0, separator).trim();
                        String value = rowText.substring(separator + 2, rowText.length() - 1).trim();
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
        return configInfo != null && configInfo.containsKey(EXECUTOR_CORE)
                ? configInfo.get(EXECUTOR_CORE) : EXECUTOR_CORE_DEFAULT;
    }

    private String getToolkitDir() {
        return configInfo != null && configInfo.containsKey(TOOLKIT_DIR)
                ? configInfo.get(TOOLKIT_DIR) : TOOLKIT_DIR_DEFAULT;
    }

    private PageNode getPage(String xmlKey, String shKey) {
        if (configInfo == null) return null;
        boolean hasXml = configInfo.containsKey(xmlKey);
        boolean hasSh = configInfo.containsKey(shKey);
        if (!hasXml && !hasSh) return null;
        PageNode page = new PageNode("");
        if (hasSh) page.setPageConfigSh(configInfo.get(shKey));
        if (hasXml) page.setPageConfigPath(configInfo.get(xmlKey));
        return page;
    }

    public PageNode getPageListConfig() { return getPage(PAGE_LIST_CONFIG, PAGE_LIST_CONFIG_SH); }
    public PageNode getFavoriteConfig() { return getPage(FAVORITE_CONFIG, FAVORITE_CONFIG_SH); }
    public PageNode getCustomTab3Config() { return getPage(CUSTOM_TAB3_CONFIG, CUSTOM_TAB3_CONFIG_SH); }
    public PageNode getCustomTab4Config() { return getPage(CUSTOM_TAB4_CONFIG, CUSTOM_TAB4_CONFIG_SH); }

    public String getBeforeStartSh() {
        return configInfo != null && configInfo.containsKey(BEFORE_START_SH)
                ? configInfo.get(BEFORE_START_SH) : BEFORE_START_SH_DEFAULT;
    }
}
