package com.omarea.krscript.downloader

import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class FileMD5 {
    private fun getFile() {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val file = File("$path/e8706cf83a2cda33dae5c40025922d75.apk")
        val md5 = getFileMD5(file)
    }

    fun getFileMD5(file: File): String? {
        if (!file.isFile) {
            return null
        }
        val digest: MessageDigest
        val `in`: FileInputStream
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance("MD5")
            `in` = FileInputStream(file)
            while (`in`.read(buffer, 0, 1024).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
            `in`.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return bytesToHexString(digest.digest())
    }

    fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder()
        if (src == null || src.isEmpty()) {
            return null
        }
        for (b in src) {
            val v = b.toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append(hv)
        }
        return stringBuilder.toString()
    }
}
