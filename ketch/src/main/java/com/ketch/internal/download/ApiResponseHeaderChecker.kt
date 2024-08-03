package com.ketch.internal.download

import com.ketch.internal.network.DownloadService

internal class ApiResponseHeaderChecker(
    private val url: String,
    private val downloadService: DownloadService,
    private val headers: HashMap<String, String> = hashMapOf()
) {
    suspend fun getHeaderValue(
        header: String
    ): String? {
        val response = downloadService.getHeadersOnly(url, headers)
        return response.headers().get(header)
    }

}
