package com.ketch

import com.ketch.internal.utils.DownloadConst
import kotlinx.serialization.Serializable

@Serializable
data class DownloadConfig(
    val connectTimeOutInMs: Long = DownloadConst.DEFAULT_VALUE_CONNECT_TIMEOUT_MS,
    val readTimeOutInMs: Long = DownloadConst.DEFAULT_VALUE_READ_TIMEOUT_MS,
    /**
     * Auto-retry download when it fails. Default true.
     */
    val autoRetry: Boolean = true,
    /**
     * Max auto-retry count. Default 3.
     */
    val maxAutoRetryCount: Int = 3,
    /**
     * Initial backoff delay in milliseconds for auto-retry. Default 30000 (30s).
     */
    val backoffDelayInMs: Long = 30000
)
