package com.ketch

sealed interface DownloadState {
    data object Queued : DownloadState
    data class Started(val length: Long) : DownloadState
    data class Progress(
        val progress: Int,
        val speedInBytePerMs: Float,
        val length: Long
    ) : DownloadState

    data object Success : DownloadState
    data class Error(val error: String?) : DownloadState
    data object Cancel : DownloadState
    data object Blocked : DownloadState
}