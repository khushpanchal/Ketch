package com.ketch.internal.network

import okhttp3.Response

internal interface DownloadService {
    suspend fun getUrl(
        url: String,
        headers: Map<String, String>,
    ): Response

    suspend fun getHeadersOnly(
        url: String,
        headers: Map<String, String>,
    ): Response

}
