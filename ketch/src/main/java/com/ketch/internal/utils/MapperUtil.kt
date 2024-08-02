package com.ketch.internal.utils

import com.ketch.DownloadModel
import com.ketch.Status
import com.ketch.internal.database.DownloadEntity

// Mapper function to convert DownloadEntity to DownloadModel
internal fun DownloadEntity.toDownloadModel() =
    DownloadModel(
        url = url,
        path = path,
        fileName = fileName,
        tag = tag,
        id = id,
        headers = WorkUtil.jsonToHashMap(headersJson),
        timeQueued = timeQueued,
        status = Status.entries.find { it.name == status } ?: Status.DEFAULT,
        total = totalBytes,
        progress = if (totalBytes.toInt() != 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
        speedInBytePerMs = speedInBytePerMs,
        lastModified = lastModified,
        eTag = eTag,
        metaData = metaData,
        failureReason = failureReason
    )
