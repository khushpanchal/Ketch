package com.ketch.internal.download

import com.ketch.Status
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil.getUniqueId
import java.util.UUID

internal data class DownloadRequest(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int = getUniqueId(url, path, fileName),
    val headers: HashMap<String, String> = hashMapOf(),
    val timeQueued: Long = System.currentTimeMillis(),
    var status: Status = Status.DEFAULT,
    var totalLength: Long = DownloadConst.DEFAULT_VALUE_LENGTH,
    var progress: Int = DownloadConst.DEFAULT_VALUE_PROGRESS,
    var speedInBytePerMs: Float = DownloadConst.DEFAULT_VALUE_SPEED,
    var uuid: UUID = UUID.randomUUID(),
    var listener: DownloadRequestListener? = null
)