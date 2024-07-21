package com.ketch.internal.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ketch.Status
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.download.DownloadTask
import com.ketch.internal.download.ETagChecker
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.notification.DownloadNotificationManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.UserAction
import com.ketch.internal.utils.WorkUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class DownloadWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {

    private var downloadNotificationManager: DownloadNotificationManager? = null
    private val dbHelper = DatabaseInstance.getInstance(context).downloadDao()

    override suspend fun doWork(): Result {

        val downloadRequest =
            WorkUtil.jsonToDownloadRequest(
                inputData.getString(DownloadConst.KEY_DOWNLOAD_REQUEST)
                    ?: return Result.failure(
                        workDataOf(ExceptionConst.KEY_EXCEPTION to ExceptionConst.EXCEPTION_FAILED_DESERIALIZE)
                    )
            )

        val downloadConfig =
            WorkUtil.jsonToDownloadConfig(
                inputData.getString(DownloadConst.KEY_DOWNLOAD_CONFIG) ?: ""
            )

        val notificationConfig =
            WorkUtil.jsonToNotificationConfig(
                inputData.getString(DownloadConst.KEY_NOTIFICATION_CONFIG) ?: ""
            )

        val id = downloadRequest.id
        val url = downloadRequest.url
        val dirPath = downloadRequest.path
        val fileName = downloadRequest.fileName
        val headers = downloadRequest.headers
        val tag = downloadRequest.tag
        val metaData = downloadRequest.metaData

        if (notificationConfig.enabled) {
            downloadNotificationManager = DownloadNotificationManager(
                context = context,
                notificationConfig = notificationConfig,
                requestId = id,
                fileName = fileName
            )
        }

        val downloadService = RetrofitInstance.getDownloadService(
            downloadConfig.connectTimeOutInMs,
            downloadConfig.readTimeOutInMs
        )

        return try {
            downloadNotificationManager?.sendUpdateNotification()?.let {
                setForeground(
                    it
                )
            }
            val latestETag =
                ETagChecker(downloadRequest.url, downloadService).getETag(headers) ?: ""
            val downloadEntity = dbHelper.find(downloadRequest.id)
            if (downloadEntity == null) {
                dbHelper.insert(
                    DownloadEntity(
                        url = url,
                        path = dirPath,
                        fileName = fileName,
                        tag = tag,
                        id = id,
                        headersJson = WorkUtil.hashMapToJson(headers),
                        timeQueued = System.currentTimeMillis(),
                        status = Status.QUEUED.toString(),
                        uuid = getId().toString(),
                        eTag = latestETag,
                        lastModified = System.currentTimeMillis(),
                        userAction = UserAction.START.toString(),
                        metaData = metaData
                    )
                )
                FileUtil.deleteFileIfExists(path = dirPath, name = fileName)
            } else {
                val existingETag = downloadEntity.eTag
                if (latestETag != existingETag) {
                    FileUtil.deleteFileIfExists(path = dirPath, name = fileName)
                    dbHelper.updateETag(id, latestETag, System.currentTimeMillis())
                }
            }

            var progressPercentage = 0

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                downloadService = downloadService
            ).download(
                headers = headers,
                onStart = { length ->
                    dbHelper.updateTotalLength(id, length, System.currentTimeMillis())
                    dbHelper.updateStatus(id, Status.STARTED.toString(), System.currentTimeMillis())

                    setProgress(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.STARTED
                        )
                    )
                },
                onProgress = { downloadedBytes, length, speed ->

                    val progress = ((downloadedBytes * 100) / length).toInt()
                    if (progressPercentage != progress) {

                        progressPercentage = progress

                        dbHelper.updateStatus(
                            id,
                            Status.PROGRESS.toString(),
                            System.currentTimeMillis()
                        )
                        dbHelper.updateProgress(
                            id,
                            downloadedBytes,
                            System.currentTimeMillis()
                        )
                        dbHelper.updateSpeed(
                            id,
                            speed,
                            System.currentTimeMillis()
                        )

                    }

                    setProgress(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.PROGRESS,
                            DownloadConst.KEY_PROGRESS to progress
                        )
                    )
                    downloadNotificationManager?.sendUpdateNotification(
                        progress = progress,
                        speedInBPerMs = speed,
                        length = length,
                        update = true
                    )?.let {
                        setForeground(
                            it
                        )
                    }
                }
            )
            dbHelper.updateTotalLength(id, totalLength, System.currentTimeMillis())
            dbHelper.updateStatus(id, Status.SUCCESS.toString(), System.currentTimeMillis())
            downloadNotificationManager?.sendDownloadSuccessNotification(totalLength)
            Result.success()
        } catch (e: Exception) {
            GlobalScope.launch {
                if (e is CancellationException) {
                    if (dbHelper.find(id)?.userAction == UserAction.PAUSE.toString()) {
                        dbHelper.updateStatus(
                            id,
                            Status.PAUSED.toString(),
                            System.currentTimeMillis()
                        )
                        downloadNotificationManager?.sendDownloadPausedNotification()
                    } else {
                        dbHelper.updateStatus(
                            id,
                            Status.CANCELLED.toString(),
                            System.currentTimeMillis()
                        )
                        FileUtil.deleteFileIfExists(dirPath, fileName)
                        downloadNotificationManager?.sendDownloadCancelledNotification()
                    }
                } else {
                    dbHelper.updateStatus(id, Status.FAILED.toString(), System.currentTimeMillis())
                    dbHelper.updateFailureReason(id, e.message ?: "", System.currentTimeMillis())
                    downloadNotificationManager?.sendDownloadFailedNotification()
                }
            }
            Result.failure(
                workDataOf(ExceptionConst.KEY_EXCEPTION to e.message)
            )
        }

    }

}
