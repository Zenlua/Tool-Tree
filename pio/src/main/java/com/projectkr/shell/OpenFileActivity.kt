package com.tool.tree

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class OpenFileActivity : AppCompatActivity() {

    companion object {
        private val mimeTypeMap = MimeTypeMap.getSingleton()

        // Hàm trả về MIME type của file
        fun getMimeType(path: String): String {
            // Thay thế toLowerCase() bằng lowercase()
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(path.lowercase())
            return mimeTypeMap.getMimeTypeFromExtension(fileExtension)
                ?: if (path.endsWith(".apk")) "application/vnd.android.package-archive" else "*/*"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra("path") ?: run {
            showToast("No file path provided")
            finish()
            return
        }

        val file = File(filePath)
        
        if (!file.exists()) {
            showToast("File does not exist")
            finish()
            return
        }

        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val mimeType = getMimeType(file.name)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showToast("No application found to open this file")
        }

        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}