package com.omarea.common.shared

import com.omarea.common.shell.RootFile

class RootFileInfo {
    constructor()
    constructor(path: String) {
        val file = RootFile.fileInfo(path)
        if (file != null) {
            this.parentDir = file.parentDir
            this.filePath = file.filePath
            this.isDirectory = file.isDirectory
        }
    }

    var parentDir: String = ""
    var filePath: String = ""
    var isDirectory: Boolean = false
    var fileSize: Long = 0

    val fileName: String
        get() {
            if (filePath.endsWith("/")) {
                return filePath.substring(0, filePath.length - 1)
            }
            return filePath
        }

    val absolutePath: String
        get() = "$parentDir/$fileName"


    fun exists(): Boolean {
        return RootFile.itemExists(this.absolutePath)
    }

    fun isFile(): Boolean {
        return !isDirectory
    }

    fun getParent(): String {
        return this.parentDir
    }

    fun getName(): String {
        return this.fileName
    }

    fun listFiles(): ArrayList<RootFileInfo> {
        if (this.isDirectory) {
            return RootFile.list(this.absolutePath)
        }
        return ArrayList()
    }

    fun length(): Long {
        return this.fileSize
    }
}
