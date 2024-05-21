package com.ketch.internal.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ketch.DownloadState
import com.ketch.Logger
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.WorkUtil.toJson
import com.ketch.internal.worker.DownloadWorker
import com.ketch.internal.worker.WorkInputData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import java.util.UUID

internal class DownloadManager(
    context: Context,
    private val logger: Logger
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    private val requests = mutableListOf<DownloadRequest>()

    @Synchronized
    fun download(downloadRequest: DownloadRequest) {
        val workInputData = WorkInputData(
            url = downloadRequest.url,
            path = downloadRequest.path,
            fileName = downloadRequest.fileName,
            id = downloadRequest.id,
            headers = downloadRequest.headers,
            notificationConfig = downloadRequest.notificationConfig,
            downloadConfig = downloadRequest.downloadConfig
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
            .setId(downloadRequest.id)
            .setConstraints(constraints)
            .build()

        requests.add(downloadRequest)
        workManager.enqueueUniqueWork(
            downloadRequest.id.toString(), ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    @Synchronized
    fun cancel(id: UUID) {
        workManager.cancelWorkById(id)
        requests.removeAll { it.id == id }
    }

    @Synchronized
    fun cancel(tag: String) {
        val list = requests.filter {
            it.tag == tag
        }

        for (req in list) {
            cancel(req.id)
        }
    }

    @Synchronized
    fun cancelAll() {
        requests.forEach {
            cancel(it.id)
        }
    }

    fun getFlowForId(id: UUID): Flow<DownloadState> {
        return workManager.getWorkInfoByIdFlow(id)
            .map { info ->
                val request = getRequestById(id)
                    ?: return@map DownloadState.Error(DownloadConst.REQUEST_NOT_FOUND)

                when (info.state) {
                    WorkInfo.State.ENQUEUED -> {
                        DownloadState.Queued
                    }

                    WorkInfo.State.RUNNING -> {
                        onWorkRunning(
                            workInfo = info,
                            request = request
                        )
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        DownloadState.Success
                    }

                    WorkInfo.State.FAILED -> {
                        onWorkFailed(
                            workInfo = info,
                            request = request
                        )
                    }

                    WorkInfo.State.BLOCKED -> {
                        DownloadState.Blocked
                    }

                    WorkInfo.State.CANCELLED -> {
                        onWorkCanceled(request = request)
                    }
                }
            }
            .runningFold(initial = DownloadState.Queued) { old: DownloadState, new ->
                new ?: old
            }
    }

    @Synchronized
    private fun getRequestById(id: UUID): DownloadRequest? {
        return requests.firstOrNull { it.id == id }
    }

    private fun onWorkRunning(
        workInfo: WorkInfo,
        request: DownloadRequest
    ): DownloadState? {
        return when (workInfo.progress.getString(DownloadConst.KEY_STATE)) {
            DownloadConst.STARTED -> {
                val totalLength =
                    workInfo.progress.getLong(
                        DownloadConst.KEY_LENGTH,
                        DownloadConst.DEFAULT_VALUE_LENGTH
                    )

                logger.log(msg = "Download Started. FileName: ${request.fileName}, URL: ${request.url}, Size in bytes: $totalLength")

                DownloadState.Started(totalLength)
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

                logger.log(msg = "Download in Progress. FileName: ${request.fileName}, URL: ${request.url}, Size in bytes: $totalLength, downloadPercent: $progress%, downloadSpeedInBytesPerMilliSeconds: $speed b/ms")
                DownloadState.Progress(
                    progress = progress,
                    speedInBytePerMs = speed,
                    length = totalLength
                )
            }

            else -> {
                null
            }
        }
    }

    private fun onWorkFailed(
        workInfo: WorkInfo,
        request: DownloadRequest
    ): DownloadState {
        val error = workInfo.outputData.getString(ExceptionConst.KEY_EXCEPTION)

        logger.log(msg = "Download Failed. FileName: ${request.fileName}, URL: ${request.url}, Reason: $error")
        return DownloadState.Error(error)
    }

    private fun onWorkCanceled(request: DownloadRequest): DownloadState {
        logger.log(msg = "Download Cancelled. FileName: ${request.fileName}, URL: ${request.url}")
        return DownloadState.Cancel
    }

}