package com.ketch.internal.database

internal interface DbHelper {
    suspend fun find(id: Int): DownloadEntity?

    suspend fun insert(entity: DownloadEntity)

    suspend fun update(entity: DownloadEntity)

    suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long)

    suspend fun updateStatus(id: Int, status: String, lastModifiedAt: Long)

    suspend fun remove(id: Int)

    suspend fun empty()

    suspend fun getAllEntity(): List<DownloadEntity>

    suspend fun getEntityTillTime(timeMillis: Long): List<DownloadEntity>
}