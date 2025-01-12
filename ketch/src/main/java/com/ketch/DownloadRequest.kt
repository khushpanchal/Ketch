package com.ketch

import com.ketch.internal.utils.FileUtil.getUniqueId
import kotlinx.serialization.Serializable

@Serializable
data class DownloadRequest(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int = getUniqueId(url, path, fileName),
    val headers: HashMap<String, String> = hashMapOf(),
    val metaData: String = ""
)
