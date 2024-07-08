package com.ketch.internal.download

import com.ketch.internal.network.DownloadService

internal class ETagChecker(
    private val url: String,
    private val downloadService: DownloadService
) {
    suspend fun getETag(): String? {
        val response = downloadService.getHeadersOnly(url)
        return response.headers().get("ETag")
    }

}