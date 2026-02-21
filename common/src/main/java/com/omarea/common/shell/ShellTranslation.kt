package com.omarea.common.shell

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.io.File
import java.util.Locale

// 从Resource解析字符串，实现输出内容多语言
class ShellTranslation(val context: Context) {
    // 示例：
    // @string:home_shell_01
    private val resRegex =
    Regex("@(string|dimen)[:/][_a-z][_0-9a-z]*", RegexOption.IGNORE_CASE)

    private val appResources: Resources by lazy {
        runCatching {
            val langFile = File(context.filesDir, "home/log/language")
            val lang = langFile
                .takeIf { it.exists() }
                ?.readText()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return@runCatching context.resources
            val locale = Locale.forLanguageTag(lang.replace("_", "-"))
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config).resources
        }.getOrElse {
            context.resources
        }
    }

    fun resolveRow(originRow: String): String? {
        var result = originRow
        val res = appResources
        resRegex.findAll(originRow).forEach { match ->
            val row = match.value   // @string:notification_ui
            val separator = if (row.contains(":")) ':' else '/'
            val type = row.substring(1, row.indexOf(separator))
            val name = row.substring(row.indexOf(separator) + 1)
    
            val id = res.getIdentifier(name, type, context.packageName)
            if (id != 0) {
                val value = when (type) {
                    "string" -> res.getString(id)
                    "dimen" -> res.getDimensionPixelSize(id).toString()
                    else -> null
                }
                if (value != null) {
                    result = result.replace(row, value)
                }
            }
        }
        return result
    }

    fun resolveRows(rows: List<String>): String {
        val builder = StringBuilder()
        var first = true
    
        for (row in rows) {
            val resolved = resolveRow(row) ?: continue
    
            if (!first) {
                builder.append('\n')
            }
            builder.append(resolved)
            first = false
        }
        return builder.toString()
    }

    fun getTranslatedResult(shellCommand: String, executor: KeepShell?): String {
        val shell = executor?: KeepShellPublic.getDefaultInstance()
        val rows = shell.doCmdSync(shellCommand).split("\n")
        return if (rows.isNotEmpty()) {
            resolveRows(rows)
        } else {
            ""
        }
    }
}