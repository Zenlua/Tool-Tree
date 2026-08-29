package com.omarea.krscript.config

import android.content.Context
import com.tool.tree.LanguageManager

object StringResRef {
    private val STRING_REF_REGEX =
        Regex("""@string[:/]([_a-zA-Z][_a-zA-Z0-9]*)""")

    fun resolve(context: Context, raw: String?): String {
        if (raw.isNullOrEmpty() || !raw.contains("@string")) {
            return raw ?: ""
        }

        val res = LanguageManager.resources(context)

        return STRING_REF_REGEX.replace(raw) { match ->
            val name = match.groupValues[1]
            try {
                val id = res.getIdentifier(name, "string", context.packageName)
                if (id != 0) res.getString(id) else match.value
            } catch (_: Exception) {
                match.value
            }
        }
    }
}