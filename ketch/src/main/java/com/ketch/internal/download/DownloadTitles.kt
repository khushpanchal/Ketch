package com.ketch.internal.download

import kotlinx.serialization.Serializable

@Serializable
data class DownloadTitles(
    var progressTitle: String? = null,
    val failedTitle: String? = null,
    val successTitle: String? = null,
    val canceledTitle: String? = null,
    val pausedTitle: String? = null,
)

@Serializable
data class DownloadContentTexts(
    var progressContentText: DownloadProgressContentText? = null,
    val failedContentText: String? = null,
    val successContentText: String? = null,
    val canceledContentText: String? = null,
    val pausedContentText: String? = null,
)

@Serializable
data class DownloadProgressContentText(
    val onlySeconds: String,
    val onlyMinutes: String,
    val onlyHours: String,
    val minutesAndSeconds: String,
    val hoursAndMinutes: String
)