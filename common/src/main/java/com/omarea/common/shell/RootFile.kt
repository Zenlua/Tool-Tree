package com.omarea.common.shell

import android.util.Log
import com.omarea.common.shared.RootFileInfo
import java.io.File

/**
 * Created by Hello on 2018/07/06.
 */

object RootFile {
    fun itemExists(path: String): Boolean {
        return File(path).exists()
    }

    fun fileExists(path: String): Boolean {
        return File(path).isFile
    }

    fun dirExists(path: String): Boolean {
        return File(path).isDirectory
    }

    fun deleteDirOrFile(path: String) {
        if (File(path).isDirectory)  File(path).deleteRecursively() else File(path).delete()
    }

    // 通过MD5比对两个文件是否相同


    // 处理像 "drwxrwx--x   3 root     root         4096 1970-07-14 17:13 vendor_de/" 这样的数据行
    private fun shellFileInfoRow(row: String, parent: String): RootFileInfo? {
        if (row.startsWith("total ")) {
            return null
        }

        try {
            val file = RootFileInfo()

            val columns = row.trim().split(" ")
            val size = columns[0]
            file.fileSize = size.toLong() * 1024

            //  8 /data/adb/modules/scene_systemless/ => /data/adb/modules/scene_systemless/
            val fileName = row.substring(row.indexOf(size) + size.length + 1)

            if (fileName == "./" || fileName == "../") {
                return null
            }

            // -F  append /dir *exe @sym |FIFO

            if (fileName.endsWith("/")) {
                file.filePath = fileName.dropLast(1)
                file.isDirectory = true
            } else if (fileName.endsWith("@")) {
                file.filePath = fileName.dropLast(1)
            } else if (fileName.endsWith("|")) {
                file.filePath = fileName.dropLast(1)
            } else if (fileName.endsWith("*")) {
                file.filePath = fileName.dropLast(1)
            } else {
                file.filePath = fileName
            }

            file.parentDir = parent

            return file
        } catch (ex: Exception) {
            return null
        }
    }

    fun list(path: String): ArrayList<RootFileInfo> {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val files = ArrayList<RootFileInfo>()
        if (dirExists(absPath)) {
            val outputInfo = KeepShellPublic.doCmdSync("busybox ls -1Fs \"$absPath\"")
            Log.d(">>>> files", outputInfo)
            if (outputInfo != "error") {
                val rows = outputInfo.split("\n")
                for (row in rows) {
                    val file = shellFileInfoRow(row, absPath)
                    if (file != null) {
                        files.add(file)
                    } else {
                        Log.e(">>>> Scene", "MapDirError Row -> $row")
                    }
                }
            }
        } else {
            Log.e(">>>> dir lost", absPath)
        }

        return files
    }

    fun fileInfo(path: String): RootFileInfo? {
        val absPath = if (path.endsWith("/")) path.subSequence(0, path.length - 1).toString() else path
        val outputInfo = KeepShellPublic.doCmdSync("busybox ls -1dFs \"$absPath\"")
        Log.d(">>>> file", outputInfo)
        if (outputInfo != "error") {
            val rows = outputInfo.split("\n")
            for (row in rows) {
                val file = shellFileInfoRow(row, absPath)
                if (file != null) {
                    file.filePath = absPath.substring(absPath.lastIndexOf("/") + 1)
                    file.parentDir = absPath.take(absPath.lastIndexOf("/"))
                    return file
                } else {
                    Log.e(">>>> Scene", "MapDirError Row -> $row")
                }
            }
        }

        return null
    }
}