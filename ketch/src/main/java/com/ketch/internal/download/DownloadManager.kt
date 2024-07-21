package com.ketch.internal.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ketch.DownloadConfig
import com.ketch.DownloadModel
import com.ketch.Logger
import com.ketch.NotificationConfig
import com.ketch.Status
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.notification.DownloadNotificationManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.FileUtil.deleteFileIfExists
import com.ketch.internal.utils.NotificationConst
import com.ketch.internal.utils.UserAction
import com.ketch.internal.utils.WorkUtil
import com.ketch.internal.utils.WorkUtil.toJson
import com.ketch.internal.worker.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.util.UUID

internal class DownloadManager(
    private val context: Context,
    private val dbHelper: DownloadDao,
    private val workManager: WorkManager
) {

    private var logger: Logger? = null
    private var downloadConfig: DownloadConfig? = null
    private var notificationConfig: NotificationConfig? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {

        scope.launch {
            workManager.getWorkInfosByTagFlow(DownloadConst.TAG_DOWNLOAD).flowOn(Dispatchers.IO)
                .collectLatest { workInfos ->
                    for (workInfo in workInfos) {
                        when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> {
                                val downloadEntity = findDownloadEntityFromUUID(workInfo.id)
                                logger?.log(msg = "Download Queued. FileName: ${downloadEntity?.fileName}, URL: ${downloadEntity?.url}")
                            }

                            WorkInfo.State.RUNNING -> {
                                val downloadEntity = findDownloadEntityFromUUID(workInfo.id)
                                when (workInfo.progress.getString(DownloadConst.KEY_STATE)) {
                                    DownloadConst.STARTED -> {
                                        logger?.log(msg = "Download Started. FileName: ${downloadEntity?.fileName}, URL: ${downloadEntity?.url}, Size in bytes: ${downloadEntity?.totalBytes}")
                                    }

                                    DownloadConst.PROGRESS -> {
                                        logger?.log(msg = "Download in Progress. FileName: ${downloadEntity?.fileName}, URL: ${downloadEntity?.url}, Size in bytes: ${downloadEntity?.totalBytes}, downloadPercent: ${if (downloadEntity != null && downloadEntity.totalBytes.toInt() != 0) ((downloadEntity.downloadedBytes * 100) / downloadEntity.totalBytes).toInt() else 0}%, downloadSpeedInBytesPerMilliSeconds: ${downloadEntity?.speedInBytePerMs} b/ms")
                                    }
                                }
                            }

                            WorkInfo.State.SUCCEEDED -> {
                                val downloadEntity = findDownloadEntityFromUUID(workInfo.id)
                                logger?.log(msg = "Download Success. FileName: ${downloadEntity?.fileName}, URL: ${downloadEntity?.url}")
                            }

                            WorkInfo.State.FAILED -> {
                                val downloadEntity = findDownloadEntityFromUUID(workInfo.id)
                                logger?.log(msg = "Download Failed. FileName: ${downloadEntity?.fileName}, URL: ${downloadEntity?.url}, Reason: ${downloadEntity?.failureReason}")
                            }

                            WorkInfo.State.CANCELLED -> {
                                val downloadEntity = findDownloadEntityFromUUID(workInfo.id)
                                if (downloadEntity?.userAction == UserAction.PAUSE.toString()) {
                                    logger?.log(msg = "Download Paused. FileName: ${downloadEntity.fileName}, URL: ${downloadEntity.url}")
                                } else if (downloadEntity?.userAction == UserAction.CANCEL.toString()) {
                                    logger?.log(msg = "Download Cancelled. FileName: ${downloadEntity.fileName}, URL: ${downloadEntity.url}")
                                }
                            }

                            WorkInfo.State.BLOCKED -> {} //no use case
                        }
                    }
                }
        }
    }

    private suspend fun download(downloadRequest: DownloadRequest) {

        val inputDataBuilder = Data.Builder()
            .putString(DownloadConst.KEY_DOWNLOAD_REQUEST, downloadRequest.toJson())
            .putString(
                DownloadConst.KEY_DOWNLOAD_CONFIG,
                (downloadConfig ?: DownloadConfig()).toJson()
            )
            .putString(
                DownloadConst.KEY_NOTIFICATION_CONFIG,
                (notificationConfig
                    ?: NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON)).toJson()
            )

        val inputData = inputDataBuilder.build()

        val constraints = Constraints
            .Builder()
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(DownloadConst.TAG_DOWNLOAD)
            .setConstraints(constraints)
            .build()

        val downloadEntity = dbHelper.find(downloadRequest.id)
        if (downloadEntity != null) {
            dbHelper.updateUserAction(
                downloadEntity.id,
                UserAction.START.toString(),
                System.currentTimeMillis()
            )
            if (downloadEntity.uuid != downloadWorkRequest.id.toString()) {
                if (downloadEntity.status != Status.QUEUED.toString() &&
                    downloadEntity.status != Status.PROGRESS.toString() &&
                    downloadEntity.status != Status.STARTED.toString()
                ) {
                    dbHelper.updateUuid(
                        downloadEntity.id,
                        downloadWorkRequest.id.toString(),
                        System.currentTimeMillis()
                    )
                    dbHelper.updateStatus(
                        downloadEntity.id,
                        Status.QUEUED.toString(),
                        System.currentTimeMillis()
                    )
                }
            }
        } else {
            dbHelper.insert(
                DownloadEntity(
                    url = downloadRequest.url,
                    path = downloadRequest.path,
                    fileName = downloadRequest.fileName,
                    tag = downloadRequest.tag,
                    id = downloadRequest.id,
                    headersJson = WorkUtil.hashMapToJson(downloadRequest.headers),
                    timeQueued = System.currentTimeMillis(),
                    status = Status.QUEUED.toString(),
                    uuid = downloadWorkRequest.id.toString(),
                    lastModified = System.currentTimeMillis(),
                    userAction = UserAction.START.toString(),
                    metaData = downloadRequest.metaData
                )
            )
        }

        workManager.enqueueUniqueWork(
            downloadRequest.id.toString(), ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    private suspend fun resume(id: Int) {
        val downloadEntity = dbHelper.find(id)
        if (downloadEntity != null) {
            dbHelper.updateUserAction(id, UserAction.RESUME.toString(), System.currentTimeMillis())
            download(
                DownloadRequest(
                    url = downloadEntity.url,
                    path = downloadEntity.path,
                    fileName = downloadEntity.fileName,
                    tag = downloadEntity.tag,
                    id = downloadEntity.id,
                    headers = WorkUtil.jsonToHashMap(downloadEntity.headersJson),
                    metaData = downloadEntity.metaData
                )
            )
        }
    }

    private suspend fun cancel(id: Int) {
        val downloadEntity = dbHelper.find(id)
        if (downloadEntity != null) {
            dbHelper.updateUserAction(id, UserAction.CANCEL.toString(), System.currentTimeMillis())
            if (downloadEntity.status == Status.PAUSED.toString() ||
                downloadEntity.status == Status.FAILED.toString()
            ) { //Edge Case: When user cancel the download in pause or fail (terminating) state as work is already cancelled.
                dbHelper.updateStatus(
                    id,
                    Status.CANCELLED.toString(),
                    System.currentTimeMillis()
                )
                deleteFileIfExists(downloadEntity.path, downloadEntity.fileName)
                DownloadNotificationManager(
                    context = context,
                    notificationConfig = notificationConfig
                        ?: NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON),
                    requestId = id,
                    fileName = downloadEntity.fileName
                ).sendDownloadCancelledNotification()
            }
        }
        workManager.cancelUniqueWork(id.toString())
    }

    private suspend fun pause(id: Int) {
        if (dbHelper.find(id) != null) {
            dbHelper.updateUserAction(id, UserAction.PAUSE.toString(), System.currentTimeMillis())
        }
        workManager.cancelUniqueWork(id.toString())
    }

    private suspend fun retry(id: Int) {
        val downloadEntity = dbHelper.find(id)
        if (downloadEntity != null) {
            dbHelper.updateUserAction(id, UserAction.RETRY.toString(), System.currentTimeMillis())
            download(
                DownloadRequest(
                    url = downloadEntity.url,
                    path = downloadEntity.path,
                    fileName = downloadEntity.fileName,
                    tag = downloadEntity.tag,
                    id = downloadEntity.id,
                    headers = WorkUtil.jsonToHashMap(downloadEntity.headersJson),
                    metaData = downloadEntity.metaData
                )
            )
        }
    }

    private suspend fun findDownloadEntityFromUUID(uuid: UUID): DownloadEntity? {
        dbHelper.getAllEntity().forEach {
            if (it.uuid == uuid.toString()) {
                return it
            }
        }
        return null
    }

    fun resumeAsync(id: Int) {
        scope.launch {
            resume(id)
        }
    }

    fun resumeAsync(tag: String) {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                if (it.tag == tag) {
                    resume(it.id)
                }
            }
        }
    }

    fun resumeAllAsync() {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                resume(it.id)
            }
        }
    }

    fun cancelAsync(id: Int) {
        scope.launch {
            cancel(id)
        }
    }

    fun cancelAsync(tag: String) {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                if (it.tag == tag) {
                    cancel(it.id)
                }
            }
        }
    }

    fun cancelAllAsync() {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                cancel(it.id)
            }
        }
    }

    fun pauseAsync(id: Int) {
        scope.launch {
            pause(id)
        }
    }

    fun pauseAsync(tag: String) {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                if (it.tag == tag) {
                    pause(it.id)
                }
            }
        }
    }

    fun pauseAllAsync() {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                pause(it.id)
            }
        }
    }

    fun retryAsync(id: Int) {
        scope.launch {
            retry(id)
        }
    }

    fun retryAsync(tag: String) {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                if (it.tag == tag) {
                    retry(it.id)
                }
            }
        }
    }

    fun retryAllAsync() {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                retry(it.id)
            }
        }
    }

    fun clearDbAsync(id: Int) {
        scope.launch {
            cancel(id)
            val downloadEntity = dbHelper.find(id)
            val path = downloadEntity?.path
            val fileName = downloadEntity?.fileName
            if (path != null && fileName != null) {
                deleteFileIfExists(path, fileName)
            }
            dbHelper.remove(id)
        }
    }

    fun clearDbAsync(tag: String) {
        scope.launch {
            dbHelper.getAllEntityByTag(tag).forEach {
                cancel(it.id)
                val downloadEntity = dbHelper.find(it.id)
                val path = downloadEntity?.path
                val fileName = downloadEntity?.fileName
                if (path != null && fileName != null) {
                    deleteFileIfExists(path, fileName)
                }
                dbHelper.remove(it.id)
            }
        }
    }

    fun clearAllDbAsync() {
        scope.launch {
            dbHelper.getAllEntity().forEach {
                cancel(it.id)
                val downloadEntity = dbHelper.find(it.id)
                val path = downloadEntity?.path
                val fileName = downloadEntity?.fileName
                if (path != null && fileName != null) {
                    deleteFileIfExists(path, fileName)
                }
            }
            dbHelper.empty()
        }
    }

    fun clearDbAsync(timeInMillis: Long) {
        scope.launch {
            dbHelper.getEntityTillTime(timeInMillis).forEach {
                cancel(it.id)
                val downloadEntity = dbHelper.find(it.id)
                val path = downloadEntity?.path
                val fileName = downloadEntity?.fileName
                if (path != null && fileName != null) {
                    deleteFileIfExists(path, fileName)
                }
                dbHelper.remove(it.id)
            }
        }
    }

    fun setConfigs(
        logger: Logger,
        downloadConfig: DownloadConfig,
        notificationConfig: NotificationConfig
    ) {
        this.logger = logger
        this.downloadConfig = downloadConfig
        this.notificationConfig = notificationConfig
    }

    fun downloadAsync(downloadRequest: DownloadRequest) {
        scope.launch {
            download(downloadRequest)
        }
    }

    fun observeDownloadById(id: Int): Flow<DownloadModel> {
        return dbHelper.getEntityByIdFlow(id).transform { entity ->
            emit(
                DownloadModel(
                    url = entity.url,
                    path = entity.path,
                    fileName = entity.fileName,
                    tag = entity.tag,
                    id = entity.id,
                    headers = WorkUtil.jsonToHashMap(entity.headersJson),
                    timeQueued = entity.timeQueued,
                    status = Status.entries.find { it.name == entity.status } ?: Status.DEFAULT,
                    total = entity.totalBytes,
                    progress = if (entity.totalBytes.toInt() != 0) ((entity.downloadedBytes * 100) / entity.totalBytes).toInt() else 0,
                    speedInBytePerMs = entity.speedInBytePerMs,
                    uuid = UUID.fromString(entity.uuid),
                    lastModified = entity.lastModified,
                    eTag = entity.eTag,
                    metaData = entity.metaData,
                    failureReason = entity.failureReason
                )
            )
        }
    }

    fun observeDownloadsByTag(tag: String): Flow<List<DownloadModel>> {
        return dbHelper.getAllEntityByTagFlow(tag).transform { entityList ->
            val downloadModelList = mutableListOf<DownloadModel>()
            entityList.forEach { entity ->
                val downloadModel = DownloadModel(
                    url = entity.url,
                    path = entity.path,
                    fileName = entity.fileName,
                    tag = entity.tag,
                    id = entity.id,
                    headers = WorkUtil.jsonToHashMap(entity.headersJson),
                    timeQueued = entity.timeQueued,
                    status = Status.entries.find { it.name == entity.status } ?: Status.DEFAULT,
                    total = entity.totalBytes,
                    progress = if (entity.totalBytes.toInt() != 0) ((entity.downloadedBytes * 100) / entity.totalBytes).toInt() else 0,
                    speedInBytePerMs = entity.speedInBytePerMs,
                    uuid = UUID.fromString(entity.uuid),
                    lastModified = entity.lastModified,
                    eTag = entity.eTag,
                    metaData = entity.metaData,
                    failureReason = entity.failureReason
                )
                downloadModelList.add(downloadModel)
            }
            downloadModelList.sortBy {
                it.timeQueued
            }
            emit(downloadModelList)
        }
    }

    fun observeAllDownloads(): Flow<List<DownloadModel>> {
        return dbHelper.getAllEntityFlow().transform { entityList ->
            val downloadModelList = mutableListOf<DownloadModel>()
            entityList.forEach { entity ->
                val downloadModel = DownloadModel(
                    url = entity.url,
                    path = entity.path,
                    fileName = entity.fileName,
                    tag = entity.tag,
                    id = entity.id,
                    headers = WorkUtil.jsonToHashMap(entity.headersJson),
                    timeQueued = entity.timeQueued,
                    status = Status.entries.find { it.name == entity.status } ?: Status.DEFAULT,
                    total = entity.totalBytes,
                    progress = if (entity.totalBytes.toInt() != 0) ((entity.downloadedBytes * 100) / entity.totalBytes).toInt() else 0,
                    speedInBytePerMs = entity.speedInBytePerMs,
                    uuid = UUID.fromString(entity.uuid),
                    lastModified = entity.lastModified,
                    eTag = entity.eTag,
                    metaData = entity.metaData,
                    failureReason = entity.failureReason
                )
                downloadModelList.add(downloadModel)
            }
            downloadModelList.sortBy {
                it.timeQueued
            }
            emit(downloadModelList)
        }
    }

}
