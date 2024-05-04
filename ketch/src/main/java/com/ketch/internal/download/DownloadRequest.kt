package com.ketch.internal.download

import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.Status
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil.getUniqueId

internal data class DownloadRequest(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String?,
    val id: Int = getUniqueId(url, path, fileName),
    val headers: HashMap<String, String> = hashMapOf(),
    var status: Status = Status.DEFAULT,
    var listener: DownloadRequestListener? = null,
    var timeQueued: Long = DownloadConst.DEFAULT_VALUE_TIME_QUEUED,
    var totalLength: Long = DownloadConst.DEFAULT_VALUE_LENGTH,
    var progress: Int = DownloadConst.DEFAULT_VALUE_PROGRESS,
    var speedInBytePerMs: Float = DownloadConst.DEFAULT_VALUE_SPEED,
    var downloadConfig: DownloadConfig = DownloadConfig(),
    val notificationConfig: NotificationConfig
)