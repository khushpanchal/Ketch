package com.ketch.internal.utils

import android.os.Environment
import android.webkit.URLUtil
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.experimental.and

internal object FileUtil {

    fun getTempFileForFile(file: File): File {
        return File(file.absolutePath + ".temp")
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

    // If file name already exist at given path, generate new file name with (1), (2) etc. suffix
    fun resolveNamingConflicts(fileName: String, path: String): String {
        var newFileName = fileName
        var file = File(path, newFileName)
        var tempFile = getTempFileForFile(file)
        var counter = 1

        while (file.exists() || tempFile.exists()) {
            val name = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".")
            newFileName = "$name ($counter).$extension"
            file = File(path, newFileName)
            tempFile = getTempFileForFile(file)
            counter++
        }

        return newFileName
    }

    fun createTempFileIfNotExists(path: String, fileName: String) {
        val file = File(path, fileName)
        val tempFile = getTempFileForFile(file)
        if (!tempFile.exists()) {
            tempFile.createNewFile()
        }
    }
}
