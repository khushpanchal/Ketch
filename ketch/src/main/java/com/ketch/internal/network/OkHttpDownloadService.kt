package com.ketch.internal.network

import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

internal class OkHttpDownloadService(
    private val callFactory: Call.Factory,
    private val baseUrl: HttpUrl,
) : DownloadService {
    init {
        require(baseUrl.pathSegments().last() == "") {
            "baseUrl must end in /: $baseUrl"
        }
    }

    override suspend fun getUrl(
        url: String,
        headers: Map<String, String>,
    ): Response {
        val request = Request.Builder()
            .get()
            .url(baseUrl.requireResolve(url))
            .headers(Headers.of(headers))
            .build()
        return callFactory.newCall(request).executeAsync()
    }

    override suspend fun getHeadersOnly(
        url: String,
        headers: Map<String, String>,
    ): Response {
        val request = Request.Builder()
            .head()
            .url(baseUrl.requireResolve(url))
            .headers(Headers.of(headers))
            .build()
        return callFactory.newCall(request).executeAsync()
    }
}