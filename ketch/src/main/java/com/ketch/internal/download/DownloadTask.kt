package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


internal class DownloadTask(
    private var url: String,
    private var path: String,
    private var fileName: String,
    private val downloadService: DownloadService,
) {

    companion object {
        private const val VALUE_200 = 200
        private const val VALUE_299 = 299
        private const val TIME_TO_TRIGGER_PROGRESS = 1500
        private const val TAG = "DownloadTask"
    }

    suspend fun download(
        headers: MutableMap<String, String> = mutableMapOf(),
        onStart: suspend (Long) -> Unit,
        onProgress: suspend (Long, Long, Float) -> Unit
    ): Long {

        val file = File(path, fileName)
        val tempFile = FileUtil.getTempFileForFile(file)

        var rangeStart = 0L
        if (tempFile.exists()) {
            rangeStart = tempFile.length()
            println("$TAG: Found existing temp file with size: $rangeStart bytes")
        }

        if (rangeStart > 0) {
            headers[DownloadConst.RANGE_HEADER] = "bytes=$rangeStart-"
            println("$TAG: Attempting to resume download from $rangeStart bytes")
        }

        var response = downloadService.getUrl(url, headers)

        if (response.code() == DownloadConst.HTTP_RANGE_NOT_SATISFY) {
            println("$TAG: Server returned 416 Range Not Satisfiable")

            if (file.exists() && file.length() > 0) {
                println("$TAG: File seems to be completely downloaded already")
                tempFile.parent?.let { FileUtil.deleteFileIfExists(it, tempFile.name) }
                return file.length()
            }

            println("$TAG: Restarting download from beginning")
            tempFile.parent?.let { FileUtil.deleteFileIfExists(it, tempFile.name) }
            headers.remove(DownloadConst.RANGE_HEADER)
            rangeStart = 0
            response = downloadService.getUrl(url, headers)
        } else if (isRedirection(response.raw().request().url().toString())) {
            println("$TAG: URL redirection detected, updating URL")
            url = response.raw().request().url().toString()
            if (rangeStart > 0) {
                println("$TAG: Continuing with range request after redirection")
                response = downloadService.getUrl(url, headers)
            }
        }

        var responseBody = response.body()

        if (response.code() !in VALUE_200..VALUE_299 || responseBody == null) {
            throw IOException(
                "Something went wrong, response code: ${response.code()}, responseBody null: ${responseBody == null}"
            )
        }

        if (rangeStart > 0 && response.code() != 206) {
            println("$TAG: Server doesn't support resume properly (no 206 response). Restarting download.")
            tempFile.parent?.let { FileUtil.deleteFileIfExists(it, tempFile.name) }
            headers.remove(DownloadConst.RANGE_HEADER)
            rangeStart = 0
            response = downloadService.getUrl(url, headers)
            responseBody.close()
            responseBody = response.body() ?: throw IOException("Response body is null")
        }

        var totalBytes = responseBody.contentLength()

        if (totalBytes < 0) throw IOException("Content Length is wrong: $totalBytes")

        var progressBytes = rangeStart
        totalBytes += rangeStart

        println("$TAG: Starting download - Total size: $totalBytes bytes, Already downloaded: $rangeStart bytes")

        responseBody.byteStream().use { inputStream ->
            FileOutputStream(tempFile, rangeStart > 0).use { outputStream ->
                onStart.invoke(totalBytes)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = inputStream.read(buffer)
                var tempBytes = 0L
                var progressInvokeTime = System.currentTimeMillis()
                var speed: Float

                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    progressBytes += bytes
                    tempBytes += bytes
                    bytes = inputStream.read(buffer)
                    val finalTime = System.currentTimeMillis()
                    if (finalTime - progressInvokeTime >= TIME_TO_TRIGGER_PROGRESS) {
                        speed = tempBytes.toFloat() / ((finalTime - progressInvokeTime).toFloat() / 1000)
                        tempBytes = 0L
                        progressInvokeTime = System.currentTimeMillis()
                        if (progressBytes > totalBytes) progressBytes = totalBytes
                        onProgress.invoke(
                            progressBytes,
                            totalBytes,
                            speed
                        )
                    }
                }
                onProgress.invoke(totalBytes, totalBytes, 0F)
            }
        }

        if (!tempFile.renameTo(file)) {
            println("$TAG: Rename failed, trying to copy file")
            tempFile.inputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.parent?.let { FileUtil.deleteFileIfExists(it, tempFile.name) }
        }

        println("$TAG: Download completed successfully - Total bytes: $totalBytes")
        return totalBytes
    }

    private fun isRedirection(requestUrl: String): Boolean {
        return requestUrl != url
    }
}
