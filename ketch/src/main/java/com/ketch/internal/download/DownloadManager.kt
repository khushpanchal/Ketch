package com.ketch.internal.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ketch.DownloadConfig
import com.ketch.DownloadModel
import com.ketch.Logger
import com.ketch.NotificationConfig
import com.ketch.Status
import com.ketch.internal.database.DbHelper
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil.deleteFileIfExists
import com.ketch.internal.utils.WorkUtil
import com.ketch.internal.utils.WorkUtil.toJson
import com.ketch.internal.worker.DownloadWorker
import com.ketch.internal.worker.WorkInputData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

internal class DownloadManager(
    context: Context,
    private val logger: Logger,
    private val downloadConfig: DownloadConfig,
    private val notificationConfig: NotificationConfig,
    private val dbHelper: DbHelper,
    private val workManager: WorkManager
) {
    private val _downloadItems = MutableStateFlow<List<DownloadModel>>(listOf())
    val downloadItems: StateFlow<List<DownloadModel>> = _downloadItems

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val idDownloadRequestMap: HashMap<Int, DownloadRequest> = hashMapOf()

    init {
        scope.launch {
            workManager.getWorkInfosByTagFlow(DownloadConst.TAG_DOWNLOAD).collect { workInfos ->
                for (workInfo in workInfos) {
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            workEnqueued(workInfo)
                        }

                        WorkInfo.State.RUNNING -> {
                            workRunning(workInfo)
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            workSucceeded(workInfo)
                        }

                        WorkInfo.State.FAILED -> {
                            workFailed(workInfo)
                        }

                        WorkInfo.State.CANCELLED -> {
                            workCancelled(workInfo)
                        }

                        WorkInfo.State.BLOCKED -> {} //no use case
                    }
                }
                val downloadModelList = mutableListOf<DownloadModel>()
                idDownloadRequestMap.values.forEach {
                    val downloadModel = DownloadModel(
                        url = it.url,
                        path = it.path,
                        fileName = it.fileName,
                        tag = it.tag,
                        id = it.id,
                        status = it.status,
                        timeQueued = it.timeQueued,
                        progress = it.progress,
                        total = it.totalLength,
                        speedInBytePerMs = it.speedInBytePerMs,
                        headers = it.headers,
                        uuid = it.uuid
                    )
                    downloadModelList.add(downloadModel)
                }
                downloadModelList.sortBy {
                    it.timeQueued
                }
                _downloadItems.emit(downloadModelList)
            }
        }

        scope.launch {
            //Load DB to memory
            val entities = dbHelper.getAllEntity()
            if(entities.isNotEmpty()) {
                entities.forEach { entity ->
                    idDownloadRequestMap[entity.id] = DownloadRequest(
                        url = entity.url,
                        path = entity.path,
                        fileName = entity.fileName,
                        tag = entity.tag,
                        id = entity.id,
                        headers = WorkUtil.jsonToHashMap(entity.headersJson),
                        status = Status.entries.find { it.name == entity.status } ?: Status.DEFAULT,
                        timeQueued = entity.timeQueued,
                        totalLength = entity.totalBytes,
                        progress = ((entity.downloadedBytes * 100)/entity.totalBytes).toInt(),
                        uuid = UUID.fromString(entity.uuid)
                    )
                }
            }
        }
    }

    private fun workCancelled(workInfo: WorkInfo) {
        val id = findIdFromUUID(workInfo.id) ?: return
        val req = idDownloadRequestMap[id] ?: return
        if(req.status == Status.PAUSED) {
            logger.log(msg = "Download Paused. FileName: ${req.fileName}, URL: ${req.url}")
            req.listener?.onPause()
            return
        }
        if (req.status != Status.CANCELLED) {
            deleteFileIfExists(req.path, req.fileName)
            req.status = Status.CANCELLED
            logger.log(msg = "Download Cancelled. FileName: ${req.fileName}, URL: ${req.url}")
            req.listener?.onCancel()
        }
    }

    private fun workFailed(workInfo: WorkInfo) {
        val id = findIdFromUUID(workInfo.id) ?: return
        val req = idDownloadRequestMap[id] ?: return
        req.status = Status.FAILED
        val error = workInfo.outputData.getString(ExceptionConst.KEY_EXCEPTION) ?: ""
        logger.log(msg = "Download Failed. FileName: ${req.fileName}, URL: ${req.url}, Reason: $error")
        req.listener?.onFailure(error)
    }

    private fun workSucceeded(workInfo: WorkInfo) {
        val id = findIdFromUUID(workInfo.id) ?: return
        val req = idDownloadRequestMap[id] ?: return
        req.status = Status.SUCCESS
        req.progress = 100
        logger.log(msg = "Download Success. FileName: ${req.fileName}, URL: ${req.url}")
        req.listener?.onSuccess()
    }

    private fun workRunning(workInfo: WorkInfo) {
        val id = workInfo.progress.getInt(DownloadConst.KEY_ID, DownloadConst.DEFAULT_VALUE_ID)
        val req = idDownloadRequestMap[id] ?: return
        when (workInfo.progress.getString(DownloadConst.KEY_STATE)) {
            DownloadConst.STARTED -> {
                val totalLength =
                    workInfo.progress.getLong(
                        DownloadConst.KEY_LENGTH,
                        DownloadConst.DEFAULT_VALUE_LENGTH
                    )
                req.status = Status.STARTED
                req.totalLength = totalLength
                logger.log(msg = "Download Started. FileName: ${req.fileName}, URL: ${req.url}, Size in bytes: $totalLength")
                req.listener?.onStart(totalLength)
            }

            DownloadConst.PROGRESS -> {
                val progress =
                    workInfo.progress.getInt(
                        DownloadConst.KEY_PROGRESS,
                        DownloadConst.DEFAULT_VALUE_PROGRESS
                    )
                val totalLength =
                    workInfo.progress.getLong(
                        DownloadConst.KEY_LENGTH,
                        DownloadConst.DEFAULT_VALUE_LENGTH
                    )
                val speed = workInfo.progress.getFloat(
                    DownloadConst.KEY_SPEED,
                    DownloadConst.DEFAULT_VALUE_SPEED
                )
                if (req.status == Status.QUEUED) { //Edge case when Progress called skipping Started
                    req.status = Status.STARTED
                    req.totalLength = totalLength
                    logger.log(msg = "Download Started. FileName: ${req.fileName}, URL: ${req.url}, Size in bytes: $totalLength")
                    req.listener?.onStart(totalLength)
                }
                req.status = Status.PROGRESS
                req.progress = progress
                req.totalLength = totalLength
                req.speedInBytePerMs = speed
                logger.log(msg = "Download in Progress. FileName: ${req.fileName}, URL: ${req.url}, Size in bytes: $totalLength, downloadPercent: $progress%, downloadSpeedInBytesPerMilliSeconds: $speed b/ms")
                req.listener?.onProgress(totalLength, progress, speed)
            }
        }
    }

    private fun workEnqueued(workInfo: WorkInfo) {
        val id = findIdFromUUID(workInfo.id) ?: return
        val req = idDownloadRequestMap[id] ?: return
        req.status = Status.QUEUED
        logger.log(msg = "Download Queued. FileName: ${req.fileName}, URL: ${req.url}")
        req.listener?.onQueue()
    }

    fun download(downloadRequest: DownloadRequest) {

        val workInputData = WorkInputData(
            url = downloadRequest.url,
            path = downloadRequest.path,
            fileName = downloadRequest.fileName,
            id = downloadRequest.id,
            headers = downloadRequest.headers,
            notificationConfig = notificationConfig,
            timeQueued = downloadRequest.timeQueued,
            tag = downloadRequest.tag,
            downloadConfig = downloadConfig
        )

        val inputDataBuilder = Data.Builder()
            .putString(DownloadConst.KEY_WORK_INPUT_DATA, workInputData.toJson())

        val inputData = inputDataBuilder.build()

        val constraints = Constraints
            .Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .addTag(DownloadConst.TAG_DOWNLOAD)
            .setConstraints(constraints)
            .build()

        downloadRequest.uuid = downloadWorkRequest.id
        idDownloadRequestMap[downloadRequest.id] = downloadRequest

        workManager.enqueueUniqueWork(
            downloadRequest.id.toString(), ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    fun cancel(id: Int) {
        val req = idDownloadRequestMap[id] ?: return
        if (req.status != Status.CANCELLED) {
            workManager.cancelUniqueWork(id.toString())
        }
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

    fun stopObserving() {
        scope.cancel()
    }

    fun pause(id: Int) {
        val req = idDownloadRequestMap[id] ?: return
        if (req.status != Status.CANCELLED && req.status != Status.PAUSED) {
            req.status = Status.PAUSED
            workManager.cancelUniqueWork(id.toString())
        }
    }

    fun pause(tag: String) {
        val list = idDownloadRequestMap.values.filter {
            it.tag == tag
        }

        for (req in list) {
            pause(req.id)
        }
    }

    fun pauseAll() {
        idDownloadRequestMap.forEach {
            pause(it.key)
        }
    }

    fun resume(id: Int) {
        val req = idDownloadRequestMap[id] ?: return
        if(req.status == Status.PAUSED || req.status == Status.CANCELLED) { //todo check
            download(req)
        }
    }

    fun retry(id: Int) {
        val req = idDownloadRequestMap[id] ?: return
        if(req.status == Status.FAILED) {
            download(req)
        }
    }

    fun resume(tag: String) {
        val list = idDownloadRequestMap.values.filter {
            it.tag == tag
        }

        for (req in list) {
            resume(req.id)
        }
    }

    fun resumeAll() {
        idDownloadRequestMap.forEach {
            resume(it.key)
        }
    }

    fun clearAllDb() {
        scope.launch {
            dbHelper.empty()
            idDownloadRequestMap.clear()
        }
    }

    fun clearDb(timeInMillis: Long) {
        scope.launch {
            dbHelper.getEntityTillTime(timeInMillis).forEach {
                dbHelper.remove(it.id)
                idDownloadRequestMap.remove(it.id)
            }
        }
    }

    fun clearDb(id: Int) {
        scope.launch {
            dbHelper.remove(id)
            idDownloadRequestMap.remove(id)
        }
    }

    private fun findIdFromUUID(uuid: UUID): Int? {
        idDownloadRequestMap.values.forEach {
            if(it.uuid == uuid) {
                return it.id
            }
        }
        return null
    }

}