package com.tool.tree

import com.omarea.krscript.model.NodeInfoBase
import java.io.Serializable

/**
 * Dữ liệu 4 tabs của MainActivity đã được tải sẵn ở SplashActivity,
 * truyền qua Intent extra "preloadedTabs" để MainActivity hiện ngay không cần loading.
 */
class MainTabsPreloadedData(
    val favorites: ArrayList<NodeInfoBase>?,
    val pages: ArrayList<NodeInfoBase>?,
    val tab3Items: ArrayList<NodeInfoBase>?,
    val tab4Items: ArrayList<NodeInfoBase>?
) : Serializable