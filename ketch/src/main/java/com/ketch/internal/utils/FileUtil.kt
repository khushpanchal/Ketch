package com.ketch.internal.utils

import android.net.Uri
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

    private fun getFileNameFromUrlUUID(url: String): String {
        val guessFileName = URLUtil.guessFileName(url, null, null)
        return UUID.randomUUID().toString() + "-" + guessFileName
    }
    /**
     * Extracts a file name from a given URL string.
     *
     * This function attempts to extract the file name from the last segment of the URL path.
     * If the URL is invalid, or if a valid file name with an extension cannot be found,
     * a UUID-based file name is generated using [getFileNameFromUrlUUID]
     *
     * @param url The URL string to extract the file name from.
     * @return The extracted file name, or a UUID-based file name if extraction fails.
     *
     * Examples:
     *  - "https://example.com/path/to/file.txt" -> "file.txt"
     *  - "https://example.com/image.jpg?query=param" -> "image.jpg"
     *  - "https://example.com/path/to/file" -> "generated_uuid_string"
     *  - "invalid_url" -> "generated_uuid_string"
     */
    fun getFileNameFromUrl(url: String): String {
        if (!URLUtil.isValidUrl(url)) {
            return getFileNameFromUrlUUID(url)
        }
        val uri = Uri.parse(url)
        val fileName = uri.lastPathSegment ?: return getFileNameFromUrlUUID(url)
        val fileExtension = fileName.substringAfterLast(".", "")
        return if (fileExtension.isNotEmpty()) {
            fileName
        } else {
            getFileNameFromUrlUUID(url)
        }
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
