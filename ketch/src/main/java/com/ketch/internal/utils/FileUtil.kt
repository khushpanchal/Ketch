package com.ketch.internal.utils

import android.os.Environment
import android.webkit.URLUtil
import java.io.File
import java.util.UUID

internal object FileUtil {

    fun getFileNameFromUrl(url: String): String {
        val guessFileName = URLUtil.guessFileName(url, null, null)
        return UUID.randomUUID().toString() + "-" + guessFileName
    }

    fun getDefaultDownloadPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }

    fun deleteFileIfExists(path: String, name: String) {
        val file = File(path, name)
        if (file.exists()) {
            file.delete()
        }
    }
}