package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil
import java.io.File
import java.io.FileOutputStream

internal class DownloadTask(
    private var url: String,
    private var path: String,
    private var fileName: String,
    private val downloadService: DownloadService,
) {

    suspend fun download(
        headers: MutableMap<String, String> = mutableMapOf(),
        onStart: suspend (Long) -> Unit,
        onProgress: suspend (Long, Long, Float) -> Unit
    ): Long {

        var rangeStart = 0L
        val file = File(path, fileName)
        val tmp = File(file.path+".tmp")

        if (tmp.exists()) {
            rangeStart = tmp.length()
        }

        if (rangeStart != 0L) {
            headers[DownloadConst.RANGE_HEADER] = "bytes=${rangeStart}-"
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


        if (response.code() !in 200..299) {
            throw RuntimeException("Something went wrong, response code: ${response.code()}")
        }

        val responseBody = response.body()
            ?: throw RuntimeException("Something went wrong, response code: ${response.code()}, response body is null")

        var totalBytes = responseBody.contentLength()

        if (totalBytes < 0) throw RuntimeException("Content Length is wrong: $totalBytes")

        var progressBytes = 0L

        totalBytes += rangeStart

        responseBody.byteStream().use { inputStream ->
            FileOutputStream(tmp, true).use { outputStream ->

                if (rangeStart != 0L) {
                    progressBytes = rangeStart
                }

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
                    if (finalTime - progressInvokeTime >= 1500) {

                        speed = tempBytes.toFloat() / ((finalTime - progressInvokeTime).toFloat())
                        tempBytes = 0L
                        progressInvokeTime = System.currentTimeMillis()
                        if (progressBytes > totalBytes) progressBytes = 100
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
        require(tmp.renameTo(file)) { "Failed to rename temp file to destination file" }
        return totalBytes
    }

    private fun isRedirection(requestUrl: String): Boolean {
        return requestUrl != url
    }
}
