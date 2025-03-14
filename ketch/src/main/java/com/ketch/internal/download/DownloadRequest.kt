package com.ketch.internal.download

import com.ketch.internal.utils.FileUtil.getUniqueId
import kotlinx.serialization.Serializable

@Serializable
internal data class DownloadRequest(
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String,
    val id: Int = getUniqueId(url, path, fileName),
    val headers: HashMap<String, String> = hashMapOf(),
    val metaData: String = "",
    val supportPauseResume: Boolean = true,
)
