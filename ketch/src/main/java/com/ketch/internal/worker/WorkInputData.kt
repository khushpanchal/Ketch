package com.ketch.internal.worker

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import java.util.UUID

internal data class WorkInputData(
    val url: String,
    val path: String,
    val fileName: String,
    val id: UUID,
    val headers: HashMap<String, String>,
    var downloadConfig: DownloadConfig,
    val notificationConfig: NotificationConfig
)
