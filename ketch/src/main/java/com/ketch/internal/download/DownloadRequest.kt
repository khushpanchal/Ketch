package com.ketch.internal.download

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.Request
import java.util.UUID

internal data class DownloadRequest(
    override val url: String,
    override val path: String,
    override val fileName: String,
    override val tag: String?,
    override val id: UUID,
    val headers: HashMap<String, String> = hashMapOf(),
    val downloadConfig: DownloadConfig = DownloadConfig(),
    val notificationConfig: NotificationConfig
): Request