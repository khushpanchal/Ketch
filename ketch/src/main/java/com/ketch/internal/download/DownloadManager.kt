package com.ketch.internal.download

import android.content.Context
import java.util.UUID

internal class DownloadManager(
    context: Context,
) {

    private val idDownloadRequestMap: HashMap<Int, DownloadRequest> = hashMapOf()
    private val uuidIdMap: HashMap<UUID, Int> = hashMapOf()

    fun download(downloadRequest: DownloadRequest) {

    }

    fun cancel(id: Int) {
        val req = idDownloadRequestMap[id] ?: return
    }

    fun cancel(tag: String) {
        val list = idDownloadRequestMap.values.filter {
            it.tag == tag
        }

        for (req in list) {
            cancel(req.id)
        }
    }

    fun cancelAll() {
        idDownloadRequestMap.forEach {
            cancel(it.key)
        }
    }

}