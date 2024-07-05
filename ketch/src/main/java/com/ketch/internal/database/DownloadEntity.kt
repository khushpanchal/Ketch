package com.ketch.internal.database

data class DownloadEntity (
    var id: Int = 0,
    var url: String = "",
    var path: String = "",
    var fileName: String = "",
    var totalBytes: Long = 0,
    var downloadedBytes: Long = 0,
    var eTag: String = "",
    var uuid: String = "",
    var status: String = "Default",
    var timeQueued: Long = 0,
    var lastModified: Long = 0,
    var headersJson: String = "random",
    var configsJson: String = "random",
    var tag: String = "random"
)