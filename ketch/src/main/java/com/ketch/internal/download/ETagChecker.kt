package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.DownloadConst

internal class ETagChecker(
    private val url: String,
    private val downloadService: DownloadService
) {
    suspend fun getETag(): String? {
        val response = downloadService.getHeadersOnly(url)
        return response.headers().get(DownloadConst.ETAG_HEADER)
    }

}