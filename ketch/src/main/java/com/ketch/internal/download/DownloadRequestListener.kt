package com.ketch.internal.download

internal interface DownloadRequestListener {
    fun onQueue()
    fun onStart(length: Long)
    fun onProgress(length: Long, progress: Int, speedInBytePerMs: Float)
    fun onSuccess()
    fun onFailure(error: String)
    fun onCancel()
    fun onPause()
}