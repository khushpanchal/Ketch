package com.ketch.internal.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ketch.internal.download.DownloadTask
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.notification.DownloadNotificationManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.NotificationHelper
import com.ketch.internal.utils.WorkUtil
import kotlinx.coroutines.CancellationException

internal class DownloadWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {

    private var downloadNotificationManager: DownloadNotificationManager? = null

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

        return try {
            downloadNotificationManager?.sendUpdateNotification()?.let {
                setForeground(
                    it
                )
            }

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                downloadService = RetrofitInstance.getDownloadService(
                    connectTimeOutInMs,
                    readTimeOutInMs
                )
            ).download(
                headers = headers,
                onStart = {
                    setProgressAsync(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.STARTED,
                            DownloadConst.KEY_ID to id,
                            DownloadConst.KEY_LENGTH to it
                        )
                    )
                },
                onProgress = { progress, length, speed ->
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
                downloadNotificationManager?.sendDownloadCancelledNotification()
            } else {
                downloadNotificationManager?.sendDownloadFailedNotification()
            }
            Result.failure(
                workDataOf(ExceptionConst.KEY_EXCEPTION to e.message)
            )
        }

    }

}

