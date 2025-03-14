package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class DownloadTask(
    private val url: String,
    private val path: String,
    private val fileName: String,
    private val supportPauseResume: Boolean = true,
    private val downloadService: DownloadService,
) {

    companion object {
        private const val VALUE_200 = 200
        private const val VALUE_299 = 299
        private const val TIME_TO_TRIGGER_PROGRESS = 1500
    }

    suspend fun download(
        headers: MutableMap<String, String> = mutableMapOf(),
        onStart: suspend (totalBytes: Long) -> Unit,
        onProgress: suspend (progressBytes: Long, totalBytes: Long, speed: Float) -> Unit
    ): Long {

        var rangeStart = 0L
        val file = File(path, fileName)
        val tempFile = FileUtil.getTempFileForFile(file)

        if (tempFile.exists()) {
            rangeStart = tempFile.length()
        }

        if (rangeStart != 0L) {
            headers[DownloadConst.RANGE_HEADER] = "bytes=$rangeStart-"
        }

        var response = downloadService.getUrl(url, headers)
        if (response.code() == DownloadConst.HTTP_RANGE_NOT_SATISFY || isRedirection(
                response.raw().request().url().toString()
            )
        ) {
            FileUtil.deleteFileIfExists(path, fileName)
            headers.remove(DownloadConst.RANGE_HEADER)
            rangeStart = 0
            response = downloadService.getUrl(url, headers)
        }

        val responseBody = response.body()

        if (response.code() !in VALUE_200..VALUE_299 ||
            responseBody == null
        ) {
            throw IOException(
                "Something went wrong, response code: ${response.code()}, responseBody null: ${responseBody == null}"
            )
        }

        var totalBytes = responseBody.contentLength()

        // pause resume not supported if we can not get content length or not supported by user
        if (totalBytes < 0 || supportPauseResume.not()) {
            totalBytes = 0
        } else {
            totalBytes += rangeStart
        }

        var progressBytes = 0L

        responseBody.byteStream().use { inputStream ->
            FileOutputStream(tempFile,true).use { outputStream ->

                if (rangeStart != 0L) {
                    progressBytes = rangeStart
                }

                onStart.invoke(totalBytes)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = inputStream.read(buffer)
                var tempBytes = 0L
                var progressInvokeTime = System.currentTimeMillis()
                var speed: Float

                onProgress.invoke(0L, 0L, 0F)

                while (bytes >= 0) {

                    outputStream.write(buffer, 0, bytes)
                    progressBytes += bytes
                    tempBytes += bytes
                    bytes = inputStream.read(buffer)
                    val finalTime = System.currentTimeMillis()
                    if (finalTime - progressInvokeTime >= TIME_TO_TRIGGER_PROGRESS) {

                        speed = tempBytes.toFloat() / ((finalTime - progressInvokeTime).toFloat())
                        tempBytes = 0L
                        progressInvokeTime = System.currentTimeMillis()
                        if (progressBytes > totalBytes) progressBytes = totalBytes
                        if(totalBytes > 0) {
                            onProgress.invoke(
                                progressBytes,
                                totalBytes,
                                speed
                            )
                        }
                    }
                }
                onProgress.invoke(totalBytes, totalBytes, 0F)
            }
        }

        require(tempFile.renameTo(file)) { "Temp file rename failed" }

        return totalBytes
    }

    private fun isRedirection(requestUrl: String): Boolean {
        return requestUrl != url
    }
}
