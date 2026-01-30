package com.omarea.krscript.model

class PageNode(currentConfigXml: String) : ClickableNode(currentConfigXml) {
    var pageConfigPath: String = ""
    var pageConfigSh: String = ""
    var onlineHtmlPage: String = ""
    // 点击后要跳转的网页链接
    var link: String = ""
    // 点击后要打开的活动
    var activity: String = ""

    // 读取页面配置前
    var beforeRead = ""
    // 读取页面配置后
    var afterRead = ""

    // 菜单选项设置
    var pageMenuOptions: ArrayList<PageMenuOption>? = null
    var pageMenuOptionsSh: String = ""
    // 处理菜单和悬浮按钮点击事件的脚本
    var pageHandlerSh:  String = ""

    // 页面加载失败
    var loadSuccess = ""
    // 页面加载成功
    var loadFail = ""
}
