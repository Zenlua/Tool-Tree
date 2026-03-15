package com.omarea.krscript.ui

import android.content.Context
import android.view.ViewGroup
import com.omarea.krscript.R
import com.omarea.krscript.model.GroupNode
import android.app.WallpaperManager
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build

class ListItemGroup(context: Context,
                    var isRootGroup: Boolean,
                    config: GroupNode) :
        ListItemView(
                context,
                if (isRootGroup) R.layout.kr_group_list_root else R.layout.kr_group_list_item,
                config) {
    protected var children = ArrayList<ListItemView>()

    fun addView(item: ListItemView): ListItemGroup {
        val content = layout.findViewById<ViewGroup>(android.R.id.content)
        content.addView(item.getView())

        children.add(item)

        return this
    }

    fun triggerActionByKey(key: String): Boolean {
        for (child in this.children) {
            if (child is ListItemClickable && child.key.equals(key)) {
                child.triggerAction()
                return true
            } else if (child is ListItemGroup && child.triggerActionByKey(key)) {
                return true
            }
        }
        return false
    }

    fun triggerActionByIndex(index: String): Boolean {
        for (child in this.children) {
            if (child is ListItemClickable && child.index.equals(index)) {
                child.triggerAction()
                return true
            }
        }
        return false
    }

    fun triggerUpdateByKey(keys: Array<String>) {
        for (key in keys) {
            if (key.equals(this.key)) {
                triggerUpdate()
            } else {
                for (child in this.children) {
                    if (child is ListItemGroup) {
                        child.triggerUpdateByKey(keys)
                    } else if (child.key.equals(key)) {
                        child.updateViewByShell()
                    }
                }
            }
        }
    }

    fun triggerUpdate() {
        for (child in this.children) {
            if (child is ListItemGroup) {
                child.triggerUpdate()
            } else {
                child.updateViewByShell()
            }
        }
    }

    init {
        title = config.title
    
        val content = layout.findViewById<ViewGroup>(android.R.id.content)
    
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable = wallpaperManager.drawable
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperDrawable != null) {
            content.setRenderEffect(
                RenderEffect.createBlurEffect(
                    25f,
                    25f,
                    Shader.TileMode.CLAMP
                )
            )
        }
    }
}
