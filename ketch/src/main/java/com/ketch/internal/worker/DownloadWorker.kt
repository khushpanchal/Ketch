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
import com.ketch.internal.utils.NotificationHelper
import com.ketch.internal.utils.WorkUtil
import kotlinx.coroutines.CancellationException

internal class DownloadWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {

    private var downloadNotificationManager: DownloadNotificationManager? = null
    private val dbHelper = DatabaseInstance.getDbHelper(context)

    override suspend fun doWork(): Result {

        val workInputData =
            WorkUtil.fromJson(
                inputData.getString(DownloadConst.KEY_WORK_INPUT_DATA)
                    ?: return Result.failure(
                        workDataOf(ExceptionConst.KEY_EXCEPTION to ExceptionConst.EXCEPTION_FAILED_DESERIALIZE)
                    )
            )

        val id = workInputData.id
        val url = workInputData.url
        val dirPath = workInputData.path
        val fileName = workInputData.fileName
        val connectTimeOutInMs = workInputData.downloadConfig.connectTimeOutInMs
        val readTimeOutInMs = workInputData.downloadConfig.readTimeOutInMs
        val headers = workInputData.headers
        val timeQueued = workInputData.timeQueued
        val tag = workInputData.tag
        val notificationEnabled = workInputData.notificationConfig.enabled

        if (notificationEnabled) {
            val notificationSmallIcon = workInputData.notificationConfig.smallIcon
            val notificationChannelName = workInputData.notificationConfig.channelName
            val notificationChannelDescription = workInputData.notificationConfig.channelDescription
            val notificationImportance = workInputData.notificationConfig.importance
            downloadNotificationManager = DownloadNotificationManager(
                context = context,
                notificationChannelName = notificationChannelName,
                notificationChannelDescription = notificationChannelDescription,
                notificationImportance = notificationImportance,
                requestId = id,
                notificationSmallIcon = notificationSmallIcon,
                fileName = fileName,
                workId = getId()
            )
        }

        val downloadService = RetrofitInstance.getDownloadService(
            connectTimeOutInMs,
            readTimeOutInMs
        )

        return try {
            downloadNotificationManager?.sendUpdateNotification()?.let {
                setForeground(
                    it
                )
            }

            val latestETag = ETagChecker(url, downloadService).getETag() ?: ""
            val downloadEntity = dbHelper.find(id)
            if(downloadEntity == null) {
                dbHelper.insert(
                    DownloadEntity(
                        id, url, dirPath, fileName, 0, 0,
                        uuid = workerParameters.id.toString(), timeQueued = timeQueued, tag = tag,
                        headersJson = WorkUtil.hashMapToJson(headers), eTag = latestETag, lastModified = System.currentTimeMillis(),
                        status = Status.QUEUED.toString()
                    )
                )
            } else {
                val existingETag = downloadEntity.eTag
                if(latestETag != existingETag) {
                    FileUtil.deleteFileIfExists(path = dirPath, name = fileName)
                }
            }

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                downloadService = downloadService
            ).download(
                headers = headers,
                onStart = {
                    dbHelper.update(
                        DownloadEntity(
                            id, url, dirPath, fileName, it, 0,
                            uuid = workerParameters.id.toString(), timeQueued = timeQueued, tag = tag,
                            headersJson = WorkUtil.hashMapToJson(headers), eTag = latestETag, status = Status.STARTED.toString(),
                            lastModified = System.currentTimeMillis()
                        )
                    )
                    setProgressAsync(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.STARTED,
                            DownloadConst.KEY_ID to id,
                            DownloadConst.KEY_LENGTH to it
                        )
                    )
                },
                onProgress = { progress, length, speed ->
                    // todo Update DB every few kb
                    dbHelper.updateStatus(id, Status.PROGRESS.toString(), System.currentTimeMillis())
                    dbHelper.updateProgress(id, (length*progress)/100, System.currentTimeMillis())

                    setProgressAsync(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.PROGRESS,
                            DownloadConst.KEY_ID to id,
                            DownloadConst.KEY_PROGRESS to progress,
                            DownloadConst.KEY_LENGTH to length,
                            DownloadConst.KEY_SPEED to speed
                        )
                    )
                    if (!NotificationHelper.isDismissedNotification(downloadNotificationManager?.getNotificationId())) {
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
                }
            )
            downloadNotificationManager?.sendDownloadSuccessNotification(totalLength)
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) {
                //todo UPDATE DB: check if possible
                //todo: issue-> on pause also it is updated to cancelled.
                dbHelper.updateStatus(id, Status.CANCELLED.toString(), System.currentTimeMillis())
//                downloadNotificationManager?.sendDownloadCancelledNotification()
            } else {
                dbHelper.updateStatus(id, Status.FAILED.toString(), System.currentTimeMillis())
                downloadNotificationManager?.sendDownloadFailedNotification()
            }
            Result.failure(
                workDataOf(ExceptionConst.KEY_EXCEPTION to e.message)
            )
        }

    }

}

