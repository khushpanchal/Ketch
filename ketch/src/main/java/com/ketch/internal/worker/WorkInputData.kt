package com.ketch.internal.worker

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig

internal data class WorkInputData(
    val url: String,
    val path: String,
    val fileName: String,
    val id: Int,
    val headers: HashMap<String, String>,
    val timeQueued: Long,
    val tag: String,
    var downloadConfig: DownloadConfig,
    val notificationConfig: NotificationConfig
)
