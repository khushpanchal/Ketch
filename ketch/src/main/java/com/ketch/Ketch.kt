package com.ketch

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.utils.DownloadLogger
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.NotificationConst
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Suppress("unused")
class Ketch private constructor(
    private val context: Context,
    private val downloadConfig: DownloadConfig,
    private val notificationConfig: NotificationConfig,
    private val logger: Logger
) {

    companion object {
        fun init(
            context: Context,
            downloadConfig: DownloadConfig = DownloadConfig(),
            notificationConfig: NotificationConfig = NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON),
            enableLogs: Boolean = false,
            logger: Logger = DownloadLogger(enableLogs)
        ): Ketch {

            return Ketch(
                context = context.applicationContext,
                downloadConfig = downloadConfig,
                notificationConfig = notificationConfig,
                logger = logger
            )
        }
    }

    private val downloadManager = DownloadManager(
        context = context,
        logger = logger
    )

    fun download(
        url: String,
        path: String = FileUtil.getDefaultDownloadPath(),
        fileName: String = FileUtil.getFileNameFromUrl(url),
        tag: String? = null,
        headers: HashMap<String, String> = hashMapOf()
    ): Request {

        if (url.isEmpty() || path.isEmpty() || fileName.isEmpty()) {
            throw RuntimeException(ExceptionConst.EXCEPTION_PARAM_MISSING)
        }

        if (notificationConfig.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val checkNotificationEnabled =
                context.applicationContext.getSystemService(NotificationManager::class.java)
                    .areNotificationsEnabled()
            if (!checkNotificationEnabled) {
                throw RuntimeException(ExceptionConst.EXCEPTION_NOTIFICATION_DISABLED)
            }
        }

        return DownloadRequest(
            id = UUID.randomUUID(),
            url = url,
            path = path,
            fileName = fileName,
            tag = tag,
            headers = headers,
            notificationConfig = notificationConfig,
            downloadConfig = downloadConfig
        ).also {
            downloadManager.download(it)
        }
    }

    fun cancel(id: UUID) {
        downloadManager.cancel(id)
    }

    fun cancel(tag: String) {
        downloadManager.cancel(tag)
    }

    fun cancelAll() {
        downloadManager.cancelAll()
    }

    fun observeDownloadById(id: UUID): Flow<DownloadState> {
        return downloadManager.getFlowForId(id)
    }

}