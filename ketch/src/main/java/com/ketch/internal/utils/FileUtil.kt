package com.ketch.internal.utils

import android.os.Environment
import android.webkit.URLUtil
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.experimental.and

internal object FileUtil {

    fun getTempFileForFile(file: File): File{
        return File(file.absolutePath+".temp")
    }

    fun getFileNameFromUrl(url: String): String {
        val guessFileName = URLUtil.guessFileName(url, null, null)
        return UUID.randomUUID().toString() + "-" + guessFileName
    }

    fun getDefaultDownloadPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }

    fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
        val string = url + File.separator + dirPath + File.separator + fileName
        val hash: ByteArray = try {
            MessageDigest.getInstance("MD5").digest(string.toByteArray(charset("UTF-8")))
        } catch (e: Exception) {
            return getUniqueIdFallback(url, dirPath, fileName)
        }
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            if (b and 0xFF.toByte() < 0x10) hex.append("0")
            hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
        }
        return hex.toString().hashCode()
    }

    private fun getUniqueIdFallback(url: String, dirPath: String, fileName: String): Int {
        return (url.hashCode() * 31 + dirPath.hashCode()) * 31 + fileName.hashCode()
    }

    fun deleteFileIfExists(path: String, name: String) {
        val file = File(path, name)
        if (file.exists()) {
            file.delete()
        }

        getTempFileForFile(file).let {
            if (it.exists()) it.delete()
        }
    }
}
