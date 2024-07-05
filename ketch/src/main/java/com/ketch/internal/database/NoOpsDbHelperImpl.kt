package com.ketch.internal.database

class NoOpsDbHelperImpl: DbHelper {
    override suspend fun find(id: Int): DownloadEntity? {
        return null
    }

    override suspend fun insert(entity: DownloadEntity) {
        
    }

    override suspend fun update(entity: DownloadEntity) {
        
    }

    override suspend fun updateProgress(id: Int, downloadedBytes: Long, lastModifiedAt: Long) {
        
    }

    override suspend fun updateStatus(id: Int, status: String, lastModifiedAt: Long) {
        
    }

    override suspend fun remove(id: Int) {
        
    }

    override suspend fun empty() {
        
    }

    override suspend fun getAllEntity(): List<DownloadEntity> {
        return emptyList()
    }

    override suspend fun getEntityTillTime(timeMillis: Long): List<DownloadEntity> {
        return emptyList()
    }
}