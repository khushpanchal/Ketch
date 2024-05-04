package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.FileUtil.deleteFileIfExists
import java.io.File

internal class DownloadTask(
    private val url: String,
    private val path: String,
    private val fileName: String,
    private val downloadService: DownloadService
) {

    suspend fun download(
        headers: Map<String, String> = mapOf(),
        onStart: suspend (Long) -> Unit,
        onProgress: suspend (Int, Long, Float) -> Unit
    ): Long {
        val responseBody = downloadService.getUrl(url, headers)
        deleteFileIfExists(path, fileName)
        val destinationFile = File(path, fileName)
        var totalBytes: Long

        responseBody.byteStream().use { inputStream ->
            destinationFile.outputStream().use { outputStream ->
                totalBytes = responseBody.contentLength()
                onStart.invoke(totalBytes)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var progressBytes = 0L
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

                    if (finalTime - progressInvokeTime >= 500) {

                        speed = tempBytes.toFloat() / ((finalTime - progressInvokeTime).toFloat())
                        tempBytes = 0L
                        progressInvokeTime = System.currentTimeMillis()
                        onProgress.invoke(
                            ((progressBytes * 100) / totalBytes).toInt(),
                            totalBytes,
                            speed
                        )

                    }
                }
                onProgress.invoke(100, totalBytes, 0F)
            }
        }

        return totalBytes
    }
}