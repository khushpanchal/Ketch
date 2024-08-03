package com.ketch.internal.download

import com.ketch.internal.network.DownloadService
import com.ketch.internal.utils.DownloadConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal class ETagChecker(
    private val url: String,
    private val downloadService: DownloadService
) {
    suspend fun getETag(
        headers: HashMap<String, String> = hashMapOf()
    ): String? {
        return try {
            val response = downloadService.getHeadersOnly(url, headers)
            response.headers().get(DownloadConst.ETAG_HEADER)
        } catch (e: Exception) {
            ""
        }
    }

}
