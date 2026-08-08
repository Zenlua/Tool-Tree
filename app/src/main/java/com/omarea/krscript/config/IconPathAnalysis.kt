package com.omarea.krscript.config

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.tool.tree.R
import com.omarea.krscript.model.ClickableNode
import com.omarea.krscript.model.TextNode

class IconPathAnalysis {
    // 获取快捷方式的图标
    fun loadLogo(context: Context, clickableNode: ClickableNode): Drawable {
        return loadLogo(context, clickableNode, true)!!
    }

    // 获取快捷方式的图标
    fun loadLogo(context: Context, clickableNode: ClickableNode, useDefault: Boolean): Drawable? {
        if (!clickableNode.logoPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.logoPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
            }
        }
        if (!clickableNode.iconPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.iconPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
            }
        }
        return if (useDefault) context.getDrawable(R.drawable.kr_shortcut_logo)!! else null
    }

    fun loadIcon(context: Context, clickableNode: ClickableNode): Drawable? {
        if (clickableNode.iconPath.isEmpty()) return null
        val paths = splitMultiPaths(clickableNode.iconPath)
        if (paths.isEmpty()) return null
        if (paths.size > 1) {
            // Danh sách nhiều đường dẫn tường minh (vd: "a.png|b.png") -> luôn ưu tiên coi là hoạt ảnh
            loadAnimatedFromPaths(context, clickableNode.pageConfigDir, paths, clickableNode.iconGifTime)?.let { return it }
        } else if (clickableNode.iconGifNum > 0) {
            loadAnimatedFrames(context, clickableNode.pageConfigDir, paths[0], clickableNode.iconGifNum, clickableNode.iconGifTime)?.let { return it }
        }
        val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(paths[0])
        inputStream?.run {
            return bitmap2Drawable(BitmapFactory.decodeStream(this)) // BitmapDrawable.createFromStream(inputStream, "")
        }
        return null
    }

    fun loadPhoto(context: Context, clickableNode: ClickableNode): Drawable? {
        if (clickableNode.photoPath.isEmpty()) return null
        val paths = splitMultiPaths(clickableNode.photoPath)
        if (paths.isEmpty()) return null
        if (paths.size > 1) {
            loadAnimatedFromPaths(context, clickableNode.pageConfigDir, paths, clickableNode.photoGifTime)?.let { return it }
        } else if (clickableNode.photoGifNum > 0) {
            loadAnimatedFrames(context, clickableNode.pageConfigDir, paths[0], clickableNode.photoGifNum, clickableNode.photoGifTime)?.let { return it }
        }
        val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(paths[0])
        inputStream?.run {
            return bitmap2Drawable(BitmapFactory.decodeStream(this))
        }
        return null
    }

    fun loadBg(context: Context, clickableNode: ClickableNode): Drawable? {
        if (!clickableNode.bgPath.isEmpty()) {
            val inputStream = PathAnalysis(context, clickableNode.pageConfigDir).parsePath(clickableNode.bgPath)
            inputStream?.run {
                return bitmap2Drawable(BitmapFactory.decodeStream(this))
            }
        }
        return null
    }

    fun loadtextPhoto(context: Context, row: TextNode.TextRow, pageDir: String): Drawable? {
        if (row.photo.isEmpty()) return null
        val paths = splitMultiPaths(row.photo)
        if (paths.isEmpty()) return null
        if (paths.size > 1) {
            loadAnimatedFromPaths(context, pageDir, paths, row.photoGifTime)?.let { return it }
        } else if (row.photoGifNum > 0) {
            loadAnimatedFrames(context, pageDir, paths[0], row.photoGifNum, row.photoGifTime)?.let { return it }
        }
        val inputStream = PathAnalysis(context, pageDir).parsePath(paths[0])
        inputStream?.run {
            return bitmap2Drawable(BitmapFactory.decodeStream(this))
        }
        return null
    }

    // Tách chuỗi cấu hình đường dẫn thành danh sách các đường dẫn riêng lẻ.
    // Hỗ trợ phân tách bằng dấu "|" trên 1 dòng, hoặc xuống dòng (chuỗi TOML nhiều dòng), hoặc cả hai.
    // Ví dụ: "path.png|test.png"  hoặc  "path.png\ntest.png"
    private fun splitMultiPaths(raw: String): List<String> {
        return raw.split('|', '\n', '\r')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    // Nạp danh sách đường dẫn tường minh (mỗi phần tử là 1 khung hình) thành hoạt ảnh kiểu GIF,
    // dùng chung thời gian mỗi khung (gif-time). Lặp vô hạn. Khung lỗi sẽ bị bỏ qua.
    private fun loadAnimatedFromPaths(context: Context, pageDir: String, paths: List<String>, frameTimeMs: Int): AnimationDrawable? {
        if (paths.size <= 1) return null
        val duration = if (frameTimeMs > 0) frameTimeMs else 300
        val anim = AnimationDrawable()
        var loadedCount = 0
        for (path in paths) {
            try {
                val inputStream = PathAnalysis(context, pageDir).parsePath(path)
                val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    anim.addFrame(bitmap2Drawable(bitmap), duration)
                    loadedCount++
                }
            } catch (ex: Exception) {
                // Bỏ qua khung hình lỗi, tiếp tục nạp các khung còn lại
            }
        }
        anim.isOneShot = false
        return if (loadedCount > 0) anim else null
    }

    // Tách phần tên (không có đuôi) và phần đuôi mở rộng của đường dẫn
    // "path/photo.png" -> ("path/photo", ".png")
    private fun splitExt(path: String): Pair<String, String> {
        val lastDot = path.lastIndexOf('.')
        val lastSlash = path.lastIndexOf('/')
        return if (lastDot > lastSlash) {
            Pair(path.substring(0, lastDot), path.substring(lastDot))
        } else {
            Pair(path, "")
        }
    }

    // Nạp chuỗi ảnh dạng "path/photo_1.png" .. "path/photo_N.png" (N = frameCount)
    // và ghép thành hoạt ảnh kiểu GIF (AnimationDrawable), lặp vô hạn.
    // Khung hình nào không đọc được sẽ bị bỏ qua (không làm hỏng cả hoạt ảnh).
    private fun loadAnimatedFrames(context: Context, pageDir: String, basePath: String, frameCount: Int, frameTimeMs: Int): AnimationDrawable? {
        if (basePath.isEmpty() || frameCount <= 0) return null
        val (baseNoExt, ext) = splitExt(basePath)
        val duration = if (frameTimeMs > 0) frameTimeMs else 300
        val anim = AnimationDrawable()
        var loadedCount = 0
        for (i in 1..frameCount) {
            val framePath = "${baseNoExt}_$i$ext"
            try {
                val inputStream = PathAnalysis(context, pageDir).parsePath(framePath)
                val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    anim.addFrame(bitmap2Drawable(bitmap), duration)
                    loadedCount++
                }
            } catch (ex: Exception) {
                // Bỏ qua khung hình lỗi, tiếp tục nạp các khung còn lại
            }
        }
        anim.isOneShot = false
        return if (loadedCount > 0) anim else null
    }

    // Bitmap转换成Drawable
    fun bitmap2Drawable(bitmap: Bitmap): Drawable {
        return BitmapDrawable(bitmap)
    }
}
