package com.omarea.common.shell

import android.content.Context
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*
import android.content.res.Configuration
import android.content.res.Resources
import java.io.File
import android.content.Intent
import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.content.ClipData

// 从Resource解析字符串，实现输出内容多语言
class ShellTranslation(val context: Context) {
    // 示例：
    // @string:home_shell_01
    private val resRegex =
    Regex("@(string|dimen)[:/][_a-z][_0-9a-z]*", RegexOption.IGNORE_CASE)

    private fun getAmHelp(): String = """
    am:[command] syntax:
    
    am:[start -a ACTION -d URI -n PACKAGE/CLASS]
    am:[startservice -n PACKAGE/CLASS]
    am:[broadcast -a ACTION]
    
    Extras:
      --es key value    String
      --ei key value    Int
      --ez key value    Boolean
      -f flags          Intent flags (decimal or 0x)
    
    Example:
      am:[start -a android.intent.action.VIEW -d https://google.com]
    """.trimIndent()

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

    private fun onAm(args: String) {
        val ctx = context
        if (args.isEmpty()) return
    
        val tokens = splitArgs(args)
        if (tokens.isEmpty()) return
    
        val cmd = tokens[0].lowercase(Locale.US)
        val subArgs = args.substring(cmd.length).trim()
    
        try {
            val intent = parseIntentArgs(subArgs)

            if (intent.action == Intent.ACTION_SEND
                || intent.action == Intent.ACTION_SEND_MULTIPLE
            ) {
                val uri =
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        ?: intent.data
                if (uri != null) {
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.clipData = ClipData.newRawUri(null, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.data = null
                }
            }

            when (cmd) {
                "start" -> {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if ((Intent.ACTION_SEND == intent.action
                            || Intent.ACTION_SEND_MULTIPLE == intent.action)
                        && intent.component == null
                    ) {
                        val chooser = Intent.createChooser(intent, null)
                        chooser.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        ctx.startActivity(chooser)
                    } else {
                        ctx.startActivity(intent)
                    }
                }
    
                "startservice" -> {
                    if (Build.VERSION.SDK_INT >= 26) {
                        ctx.startForegroundService(intent)
                    } else {
                        ctx.startService(intent)
                    }
                }
    
                "broadcast" -> {
                    ctx.sendBroadcast(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun parseIntentArgs(args: String): Intent {
        val intent = Intent()
        val tokens = splitArgs(args)
    
        var i = 0
        while (i < tokens.size) {
            when (tokens[i]) {
    
                // action
                "-a" -> {
                    if (i + 1 < tokens.size) {
                        intent.action = tokens[++i]
                    }
                }
    
                // data uri
                "-d" -> {
                    if (i + 1 < tokens.size) {
                        var value = stripQuote(tokens[++i])
                        val uri = when {
                            value.contains("://") -> Uri.parse(value)
                            value.startsWith("/") -> Uri.fromFile(File(value))
                            else -> Uri.parse(value)
                        }
                        intent.data = uri
                    }
                }
    
                // mime type
                "-t" -> {
                    if (i + 1 < tokens.size) {
                        intent.type = tokens[++i]
                    }
                }
    
                // component
                "-n" -> {
                    if (i + 1 < tokens.size) {
                        val cn = tokens[++i].split("/", limit = 2)
                        if (cn.size == 2) {
                            intent.component = ComponentName(cn[0], cn[1])
                        }
                    }
                }
    
                // package
                "-p" -> {
                    if (i + 1 < tokens.size) {
                        intent.`package` = tokens[++i]
                    }
                }
    
                // category
                "-c" -> {
                    if (i + 1 < tokens.size) {
                        intent.addCategory(tokens[++i])
                    }
                }
    
                // flags
                "-f" -> {
                    if (i + 1 < tokens.size) {
                        val v = tokens[++i]
                        val flags = if (v.startsWith("0x")) {
                            v.substring(2).toInt(16)
                        } else {
                            v.toInt()
                        }
                        intent.addFlags(flags)
                    }
                }
    
                // String extra
                "--es" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        val value = tokens[++i]
                        intent.putExtra(key, value)
                    }
                }
    
                // int extra
                "--ei" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        try {
                            intent.putExtra(key, tokens[++i].toInt())
                        } catch (_: Exception) {}
                    }
                }
    
                // long extra
                "--el" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        try {
                            intent.putExtra(key, tokens[++i].toLong())
                        } catch (_: Exception) {}
                    }
                }
    
                // boolean extra
                "--ez" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        intent.putExtra(key, tokens[++i].toBoolean())
                    }
                }
    
                // float extra
                "--ef" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        try {
                            intent.putExtra(key, tokens[++i].toFloat())
                        } catch (_: Exception) {}
                    }
                }

                // Uri extra
                "--eu" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        var value = stripQuote(tokens[++i])
                        val uri = when {
                            value.contains("://") -> Uri.parse(value)
                            value.startsWith("/") -> Uri.fromFile(File(value))
                            else -> Uri.parse(value)
                        }
                        intent.putExtra(key, uri)
                    }
                }

                // double extra
                "--ed" -> {
                    if (i + 2 < tokens.size) {
                        val key = tokens[++i]
                        try {
                            intent.putExtra(key, tokens[++i].toDouble())
                        } catch (_: Exception) {}
                    }
                }
    
                // null string
                "--esn" -> {
                    if (i + 1 < tokens.size) {
                        intent.putExtra(tokens[++i], null as String?)
                    }
                }
    
                // String[]
                "--esa" -> {
                    if (i + 1 < tokens.size) {
                        val key = tokens[++i]
                        val list = ArrayList<String>()
                        while (i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                            list.add(tokens[++i])
                        }
                        intent.putExtra(key, list.toTypedArray())
                    }
                }
    
                // int[]
                "--eia" -> {
                    if (i + 1 < tokens.size) {
                        val key = tokens[++i]
                        val list = ArrayList<Int>()
                        while (i + 1 < tokens.size && !tokens[i + 1].startsWith("-")) {
                            try {
                                list.add(tokens[++i].toInt())
                            } catch (_: Exception) {}
                        }
                        intent.putExtra(key, list.toIntArray())
                    }
                }
    
                // grant uri permissions
                "--grant-read-uri-permission" -> {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
    
                "--grant-write-uri-permission" -> {
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
            }
            i++
        }
        return intent
    }
    
    private fun splitArgs(args: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var inQuote = false
        var quoteChar = '\u0000'
        var i = 0
    
        while (i < args.length) {
            val c = args[i]
    
            if (inQuote) {
                when {
                    c == quoteChar -> {
                        inQuote = false
                    }
                    c == '\\' && i + 1 < args.length -> {
                        when (val n = args[++i]) {
                            'n' -> cur.append('\n')
                            't' -> cur.append('\t')
                            '\\' -> cur.append('\\')
                            '"' -> cur.append('"')
                            '\'' -> cur.append('\'')
                            else -> cur.append(n)
                        }
                    }
                    else -> cur.append(c)
                }
            } else {
                when {
                    c == '"' || c == '\'' -> {
                        inQuote = true
                        quoteChar = c
                    }
                    c.isWhitespace() -> {
                        if (cur.isNotEmpty()) {
                            out.add(cur.toString())
                            cur.setLength(0)
                        }
                    }
                    else -> cur.append(c)
                }
            }
            i++
        }
    
        if (cur.isNotEmpty()) {
            out.add(cur.toString())
        }
    
        return out
    }

    fun resolveRow(originRow: String): String? {

        val trimmed = originRow.trim()
        if (trimmed.startsWith("am:[", true) && trimmed.endsWith("]")) {
            val args = trimmed
                .removePrefix("am:[")
                .removeSuffix("]")
                .trim()
            if (args.equals("help", true)) {
                return getAmHelp()
            } else if (args.isNotEmpty()) {
                onAm(args)
            }
            return ""
        }

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