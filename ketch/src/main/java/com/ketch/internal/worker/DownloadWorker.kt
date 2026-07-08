package com.ketch.internal.worker

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ketch.KetchException
import com.ketch.NotificationConfig
import com.ketch.Status
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.download.ApiResponseHeaderChecker
import com.ketch.internal.download.DownloadTask
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.notification.DownloadNotificationManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.UserAction
import com.ketch.internal.utils.WorkUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.net.UnknownHostException

internal class DownloadWorker(
    context: Context,
    workerParameters: WorkerParameters
) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        private const val MAX_PERCENT = 100
        private const val TAG = "DownloadWorker"
    }

    private var downloadNotificationManager: DownloadNotificationManager? = null
    private val downloadDao = DatabaseInstance.getInstance(applicationContext).downloadDao()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val downloadRequest =
            WorkUtil.jsonToDownloadRequest(
                inputData.getString(DownloadConst.KEY_DOWNLOAD_REQUEST) ?: ""
            )

        val notificationConfig =
            WorkUtil.jsonToNotificationConfig(
                inputData.getString(DownloadConst.KEY_NOTIFICATION_CONFIG) ?: ""
            )

        val id = downloadRequest.id
        val fileName = downloadRequest.fileName

        val notificationManager = requireNotificationManager(notificationConfig, id, fileName)
        return notificationManager.createUpdateNotification()!!
    }

    override suspend fun doWork(): Result {

        val downloadRequest =
            WorkUtil.jsonToDownloadRequest(
                inputData.getString(DownloadConst.KEY_DOWNLOAD_REQUEST)
                    ?: return Result.failure(
                        workDataOf(ExceptionConst.KEY_EXCEPTION to ExceptionConst.EXCEPTION_FAILED_DESERIALIZE)
                    )
            )

        val notificationConfig =
            WorkUtil.jsonToNotificationConfig(
                inputData.getString(DownloadConst.KEY_NOTIFICATION_CONFIG) ?: ""
            )

        val downloadConfig =
            WorkUtil.jsonToDownloadConfig(
                inputData.getString(DownloadConst.KEY_DOWNLOAD_CONFIG) ?: ""
            )

        val id = downloadRequest.id
        val url = downloadRequest.url
        val dirPath = downloadRequest.path
        val fileName = downloadRequest.fileName
        val headers = downloadRequest.headers
        val supportPauseResume = downloadRequest.supportPauseResume // in case of false, we will not store total length info in DB

        if (notificationConfig.enabled) {
            requireNotificationManager(notificationConfig, id, fileName)
        }

        val downloadService = RetrofitInstance.getDownloadService()

        return try {
            if (!isStopped) {
                // WORKAROUND: Android 14+ has a race condition where calling setForeground()
                // immediately after an expedited worker starts can trigger a
                // ForegroundServiceStartNotAllowedException. This small delay allows
                // the system to settle and correctly register the expedited exemption for this PID.
                delay(1000)
                downloadNotificationManager?.createUpdateNotification()?.let { info ->
                    trySetForeground(info)
                }
                downloadNotificationManager?.clearPreviousNotifications()
            }

            var latestETag = ""
            var latestContentLength = 0L

            if (isStopped) return Result.retry()

            // DNS/Network Retry Logic for Android 15 background restrictions
            var attempts = 0
            while (attempts < 3) {
                try {
                    val headerChecker =
                        ApiResponseHeaderChecker(downloadRequest.url, downloadService, headers)
                    latestETag = headerChecker.getHeaderValue(DownloadConst.ETAG_HEADER) ?: ""
                    latestContentLength =
                        headerChecker.getHeaderValue(DownloadConst.CONTENT_LENGTH)?.toLongOrNull() ?: 0L
                    break
                } catch (e: UnknownHostException) {
                    attempts++
                    if (attempts >= 3) throw e
                    Timber.tag(TAG).w("DNS lookup failed, retrying in 2s... (attempt $attempts)")
                    delay(2000)
                }
            }

            val existingEntity = downloadDao.find(id)
            val existingETag = existingEntity?.eTag ?: ""
            val existingTotalBytes = existingEntity?.totalBytes ?: 0L
            val existingStatus = existingEntity?.status ?: ""

            // Check for early completion
            if (existingStatus == Status.SUCCESS.toString() &&
                File(dirPath, fileName).exists() &&
                (latestETag.isEmpty() || latestETag == existingETag) &&
                (latestContentLength == 0L || latestContentLength == existingTotalBytes)
            ) {
                downloadNotificationManager?.sendDownloadSuccessNotification(
                    totalLength = existingTotalBytes
                )
                return Result.success()
            }

            // Check for server-side updates (ETag or Content-Length changed)
            val eTagChanged = latestETag.isNotEmpty() && existingETag.isNotEmpty() && latestETag != existingETag
            val sizeChanged = latestContentLength != 0L && existingTotalBytes != 0L && latestContentLength != existingTotalBytes

            if (eTagChanged || sizeChanged || (latestETag.isNotEmpty() && existingETag.isEmpty())) {
                FileUtil.deleteFileIfExists(path = dirPath, name = fileName)
                FileUtil.createTempFileIfNotExists(path = dirPath, fileName = fileName)
                existingEntity?.copy(
                    eTag = latestETag,
                    totalBytes = latestContentLength,
                    downloadedBytes = 0,
                    lastModified = System.currentTimeMillis()
                )?.let { downloadDao.update(it) }
            }

            var progressPercentage = -1

            val totalLength = DownloadTask(
                url = url,
                path = dirPath,
                fileName = fileName,
                supportPauseResume = supportPauseResume,
                downloadService = downloadService
            ).download(
                headers = headers,
                onStart = { length ->

                    downloadDao.find(id)?.copy(
                        totalBytes = length,
                        status = Status.STARTED.toString(),
                        lastModified = System.currentTimeMillis()
                    )?.let { downloadDao.update(it) }

                    setProgress(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.STARTED
                        )
                    )
                },
                onProgress = { downloadedBytes, length, speed ->

                    val progress = if (length != 0L) {
                        ((downloadedBytes * 100) / length).toInt()
                    } else {
                        0
                    }

                    if (progressPercentage != progress) {

                        progressPercentage = progress

                        downloadDao.find(id)?.copy(
                            downloadedBytes = downloadedBytes,
                            speedInBytePerMs = speed,
                            status = Status.PROGRESS.toString(),
                            lastModified = System.currentTimeMillis()
                        )?.let { downloadDao.update(it) }

                    }

                    setProgress(
                        workDataOf(
                            DownloadConst.KEY_STATE to DownloadConst.PROGRESS,
                            DownloadConst.KEY_PROGRESS to progress
                        )
                    )

                    if (!isStopped && progress < MAX_PERCENT) {
                        downloadNotificationManager?.createUpdateNotification(
                            progress = progress,
                            speedInBPerMs = speed,
                            length = length,
                            update = true
                        )?.let { info ->
                            // Use direct NotificationManager for updates to avoid repeated startService/setForeground calls
                            // which are prone to BackgroundServiceStartNotAllowedException on ACTION_STOP_FOREGROUND
                            updateNotification(info)
                        }
                    }
                }
            )

            val total = if (totalLength > 0) totalLength else File(dirPath, fileName).length()

            downloadDao.find(id)?.copy(
                totalBytes = total,
                downloadedBytes = total,
                status = Status.SUCCESS.toString(),
                lastModified = System.currentTimeMillis()
            )?.let { downloadDao.update(it) }

            downloadNotificationManager?.sendDownloadSuccessNotification(
                totalLength = total
            )
            Result.success()
        } catch (e: Exception) {
            withContext(NonCancellable + Dispatchers.IO) {
                val entity = downloadDao.find(id)
                val isRetryable = e !is CancellationException && 
                                e !is KetchException.NonRetryableException &&
                                runAttemptCount < downloadConfig.maxAutoRetryCount &&
                                downloadConfig.autoRetry

                if (isRetryable) {
                    if (entity != null && isAnotherActiveOrSuccessful(entity)) {
                        downloadDao.update(
                            entity.copy(
                                status = Status.FAILED.toString(),
                                failureReason = "Skipped retry. Another entry for same file is already active or successful.",
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    } else {
                        downloadDao.find(id)?.copy(
                            failureReason = e.message ?: "",
                            lastModified = System.currentTimeMillis(),
                            status = Status.RETRY_QUEUED.toString()
                        )?.let { downloadDao.update(it) }
                    }
                } else if (e is CancellationException) {
                    if (downloadDao.find(id)?.userAction == UserAction.PAUSE.toString()) {

                        downloadDao.find(id)?.copy(
                            status = Status.PAUSED.toString(),
                            lastModified = System.currentTimeMillis()
                        )?.let { downloadDao.update(it) }
                        val downloadEntity = downloadDao.find(id)
                        if (downloadEntity != null) {
                            val currentProgress = if (downloadEntity.totalBytes != 0L) {
                                ((downloadEntity.downloadedBytes * MAX_PERCENT) / downloadEntity.totalBytes).toInt()
                            } else {
                                0
                            }
                            downloadNotificationManager?.sendDownloadPausedNotification(
                                currentProgress = currentProgress
                            )
                        }

                    } else {

                        downloadDao.find(id)?.copy(
                            status = Status.CANCELLED.toString(),
                            lastModified = System.currentTimeMillis()
                        )?.let { downloadDao.update(it) }
                        FileUtil.deleteFileIfExists(dirPath, fileName)
                        downloadNotificationManager?.sendDownloadCancelledNotification()

                    }
                } else {
                    // Final Failure
                    downloadDao.find(id)?.copy(
                        status = Status.FAILED.toString(),
                        failureReason = e.message ?: "",
                        lastModified = System.currentTimeMillis()
                    )?.let { downloadDao.update(it) }
                    val downloadEntity = downloadDao.find(id)
                    if (downloadEntity != null) {
                        val currentProgress = if (downloadEntity.totalBytes != 0L) {
                            ((downloadEntity.downloadedBytes * MAX_PERCENT) / downloadEntity.totalBytes).toInt()
                        } else {
                            0
                        }
                        downloadNotificationManager?.sendDownloadFailedNotification(
                            currentProgress = currentProgress
                        )
                    }
                }
            }
            
            val shouldRetry = e !is CancellationException && 
                              e !is KetchException.NonRetryableException &&
                              runAttemptCount < downloadConfig.maxAutoRetryCount &&
                              downloadConfig.autoRetry

            if (shouldRetry) {
                val entity = downloadDao.find(id)
                if (entity != null && isAnotherActiveOrSuccessful(entity)) {
                    Result.failure(
                        workDataOf(ExceptionConst.KEY_EXCEPTION to "Skipped retry. Another entry active.")
                    )
                } else {
                    Result.retry()
                }
            } else {
                Result.failure(
                    workDataOf(ExceptionConst.KEY_EXCEPTION to e.message)
                )
            }
        }

    }

    private fun requireNotificationManager(notificationConfig: NotificationConfig, id: Int, fileName: String): DownloadNotificationManager {
        downloadNotificationManager = DownloadNotificationManager(
            context = applicationContext,
            notificationConfig = notificationConfig,
            requestId = id,
            fileName = fileName
        )
        return downloadNotificationManager!!
    }

    private suspend fun isAnotherActiveOrSuccessful(entity: DownloadEntity): Boolean {
        val activeStatuses = listOf(
            Status.QUEUED.name,
            Status.RETRY_QUEUED.name,
            Status.STARTED.name,
            Status.PROGRESS.name,
            Status.SUCCESS.name
        )
        return downloadDao.countOtherActiveOrSuccessful(
            entity.url,
            entity.path,
            entity.id,
            activeStatuses
        ) > 0
    }

    private suspend fun trySetForeground(foregroundInfo: ForegroundInfo) {
        // Non-expedited jobs (regular workers) cannot transition to foreground from background on Android 12+.
        // However, we rely on the try-catch block below to handle this gracefully if the system denies it,
        // because determining "isExpedited" can be version-dependent.
        try {
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            val exceptionName = e.javaClass.name
            if (e is IllegalStateException || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (exceptionName.contains("BackgroundServiceStartNotAllowedException") || exceptionName.contains("ForegroundServiceStartNotAllowedException")))) {
                Timber.tag(TAG).w("Failed to set foreground state, falling back to direct notification: ${e.message}")
                // Fallback: Show a regular notification if foreground transition is denied
                updateNotification(foregroundInfo)
            } else {
                throw e
            }
        }
    }

    private fun updateNotification(foregroundInfo: ForegroundInfo) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(foregroundInfo.notificationId, foregroundInfo.notification)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update notification directly")
        }
    }

}
