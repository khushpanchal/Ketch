package com.khush.sample

import java.util.UUID

data class DownloadModel(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String?,
    val id: UUID,
    val status: Status,
    val timeQueued: Long,
    val progress: Int,
    val total: Long,
    val speedInBytePerMs: Float
)