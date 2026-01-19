package com.ketch.internal.network

import com.ketch.internal.utils.DownloadConst
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object RetrofitInstance {

    @Volatile
    private var downloadService: DownloadService? = null

    fun getDownloadService(
        callFactory: Call.Factory =
            OkHttpClient
                .Builder()
                .connectTimeout(DownloadConst.DEFAULT_VALUE_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DownloadConst.DEFAULT_VALUE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
    ): DownloadService {
        if (downloadService == null) {
            synchronized(this) {
                if (downloadService == null) {
                    val baseUrl = HttpUrl.get(DownloadConst.BASE_URL)
                    downloadService = OkHttpDownloadService(callFactory, baseUrl)
                }
            }
        }
        return downloadService!!
    }
}
