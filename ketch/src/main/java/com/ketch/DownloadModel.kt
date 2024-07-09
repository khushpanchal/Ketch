package com.ketch

import java.util.UUID

data class DownloadModel(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int,
    val status: Status,
    val timeQueued: Long,
    val progress: Int,
    val total: Long,
    val speedInBytePerMs: Float,
    val headers: HashMap<String, String>,
    val uuid: UUID,
    val eTag: String
)