package com.omarea.krscript.config

import android.content.Context
import androidx.core.content.ContextCompat

/**
 * Hỗ trợ nhập màu dạng tham chiếu resource, ví dụ:
 *   @color/colorAccent
 *   @color:colorAccent
 *   @android:color/white
 *
 * Dùng cùng cơ chế @string/... đã có ở StringResRef, áp dụng cho tham số
 * kiểu "color" (ParamsColorPicker) để người dùng có thể tham chiếu tới màu
 * đã khai báo trong colors.xml (của app hoặc hệ thống) thay vì phải nhập
 * mã hex trực tiếp.
 */
object ColorResRef {
    // @android:color/name  hoặc  @color/name  hoặc  @color:name
    private val COLOR_REF_REGEX =
        Regex("""^@(android:)?color[:/]([_a-zA-Z][_a-zA-Z0-9.]*)$""")

    /**
     * Trả về true nếu chuỗi có dạng tham chiếu màu "@..."
     */
    fun isColorRef(raw: String?): Boolean {
        if (raw.isNullOrEmpty()) return false
        return COLOR_REF_REGEX.matches(raw.trim())
    }

    /**
     * Phân giải chuỗi tham chiếu màu thành mã màu (Int, dạng ARGB).
     * Trả về null nếu không phải tham chiếu hợp lệ hoặc không tìm thấy resource.
     */
    fun resolve(context: Context, raw: String?): Int? {
        if (raw.isNullOrEmpty()) return null
        val match = COLOR_REF_REGEX.matchEntire(raw.trim()) ?: return null
        val isAndroidNs = match.groupValues[1].isNotEmpty()
        val name = match.groupValues[2]

        return try {
            val packageName = if (isAndroidNs) "android" else context.packageName
            val id = context.resources.getIdentifier(name, "color", packageName)
            if (id != 0) ContextCompat.getColor(context, id) else null
        } catch (_: Exception) {
            null
        }
    }
}
