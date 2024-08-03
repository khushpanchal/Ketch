package com.ketch

import com.ketch.internal.utils.DownloadConst
import kotlinx.serialization.Serializable

@Serializable
data class DownloadConfig(
    val connectTimeOutInMs: Long = DownloadConst.DEFAULT_VALUE_CONNECT_TIMEOUT_MS,
    val readTimeOutInMs: Long = DownloadConst.DEFAULT_VALUE_READ_TIMEOUT_MS
)
