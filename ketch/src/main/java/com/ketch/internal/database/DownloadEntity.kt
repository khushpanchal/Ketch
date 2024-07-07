package com.ketch.internal.database

import com.ketch.Status

data class DownloadEntity (
    var id: Int = 0,
    var url: String = "",
    var path: String = "",
    var fileName: String = "",
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var eTag: String = "",
    var uuid: String = "",
    var status: String = Status.DEFAULT.toString(),
    var timeQueued: Long = 0,
    var lastModified: Long = 0,
    var headersJson: String = "",
    var tag: String = ""
)