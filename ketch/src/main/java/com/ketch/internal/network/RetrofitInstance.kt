package com.ketch.internal.network

import com.ketch.internal.utils.DownloadConst
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

internal object RetrofitInstance {

    @Volatile
    private var downloadService: DownloadService? = null

    fun getDownloadService(
        okHttpClient: OkHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(DownloadConst.DEFAULT_VALUE_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DownloadConst.DEFAULT_VALUE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build()
    ): DownloadService {
        if (downloadService == null) {
            synchronized(this) {
                if (downloadService == null) {
                    downloadService = Retrofit
                        .Builder()
                        .baseUrl(DownloadConst.BASE_URL)
                        .client(okHttpClient)
                        .build()
                        .create(DownloadService::class.java)
                }
            }
        }
        return downloadService!!
    }
}
