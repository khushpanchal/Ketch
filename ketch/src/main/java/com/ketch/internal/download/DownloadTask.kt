package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import kotlinx.coroutines.delay
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
        onProgress: suspend (Int, Long, Float) -> Unit
    ): Long {

        var rangeStart = 0L
        val file = File(path, fileName)

        val destinationFile = File(path, fileName)

        val out = FileOutputStream(destinationFile, true)
        if(file.exists()) {
            rangeStart = file.length()
        }

        if(rangeStart != 0L) {
            headers["Range"] = "bytes=${rangeStart}-"
        }

        val responseBody = downloadService.getUrl(url, headers)

        var totalBytes: Long

        responseBody.byteStream().use { inputStream ->
            out.use { outputStream ->
                var progressBytes = 0L
                totalBytes = responseBody.contentLength() + rangeStart

                if(rangeStart != 0L) {
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
                    //TODO remove
                    delay(50)
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
//                        dbHelper.updateProgress(id, randomAccessFile.length(), System.currentTimeMillis())

                    }
                }
                onProgress.invoke(100, totalBytes, 0F)
            }
        }

        return totalBytes
    }
}