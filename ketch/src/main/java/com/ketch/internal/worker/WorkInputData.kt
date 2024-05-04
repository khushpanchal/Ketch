package com.ketch.internal.worker

import com.ketch.DownloadConfig

internal data class WorkInputData(
    val url: String,
    val path: String,
    val fileName: String,
    val id: Int,
    val headers: HashMap<String, String>,
    var downloadConfig: DownloadConfig,
)
