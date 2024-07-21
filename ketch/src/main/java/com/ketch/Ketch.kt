package com.ketch

import android.content.Context
import androidx.work.WorkManager
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.download.ETagChecker
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.utils.DownloadLogger
import com.ketch.internal.utils.ExceptionConst
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.NotificationConst
import kotlinx.coroutines.flow.Flow

class Ketch private constructor(
    private val context: Context,
    private var downloadConfig: DownloadConfig = DownloadConfig(),
    private var notificationConfig: NotificationConfig = NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON),
    private var logger: Logger = DownloadLogger(false)
) {

    companion object {

        private lateinit var ketch: Ketch

        fun getInstance(
            context: Context
        ): Ketch {
            if (!::ketch.isInitialized) {
                ketch = Ketch(
                    context = context.applicationContext
                )
            }
            return ketch
        }
    }

    private val downloadManager = DownloadManager(
        context = context,
        dbHelper = DatabaseInstance.getInstance(context).downloadDao(),
        workManager = WorkManager.getInstance(context.applicationContext)
    )

    fun initConfig(
        downloadConfig: DownloadConfig = DownloadConfig(),
        notificationConfig: NotificationConfig = NotificationConfig(smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON),
        enableLogs: Boolean = false,
        logger: Logger = DownloadLogger(enableLogs)
    ) {
        this.downloadConfig = downloadConfig
        this.notificationConfig = notificationConfig
        this.logger = logger
        downloadManager.setConfigs(
            logger, downloadConfig, notificationConfig
        )
    }

    fun download(
        url: String,
        path: String = FileUtil.getDefaultDownloadPath(),
        fileName: String = FileUtil.getFileNameFromUrl(url),
        tag: String = "",
        metaData: String = "",
        headers: HashMap<String, String> = hashMapOf()
    ): Int {

        if (url.isEmpty() || path.isEmpty() || fileName.isEmpty()) {
            throw RuntimeException(ExceptionConst.EXCEPTION_PARAM_MISSING)
        }

        val downloadRequest = DownloadRequest(
            url = url,
            path = path,
            fileName = fileName,
            tag = tag,
            headers = headers,
            metaData = metaData
        )
        downloadManager.downloadAsync(downloadRequest)
        return downloadRequest.id
    }

    fun cancel(id: Int) {
        downloadManager.cancelAsync(id)
    }

    fun cancel(tag: String) {
        downloadManager.cancelAsync(tag)
    }

    fun cancelAll() {
        downloadManager.cancelAllAsync()
    }

    fun observeDownloads(): Flow<List<DownloadModel>> {
        return downloadManager.observeAllDownloads()
    }

    fun observeDownloadById(id: Int): Flow<DownloadModel> {
        return downloadManager.observeDownloadById(id)
    }

    fun observeDownloadByTag(tag: String): Flow<List<DownloadModel>> {
        return downloadManager.observeDownloadsByTag(tag)
    }

    fun pause(id: Int) {
        downloadManager.pauseAsync(id)
    }

    fun pause(tag: String) {
        downloadManager.pauseAsync(tag)
    }

    fun pauseAll() {
        downloadManager.pauseAllAsync()
    }

    fun resume(id: Int) {
        downloadManager.resumeAsync(id)
    }

    fun resume(tag: String) {
        downloadManager.resumeAsync(tag)
    }

    fun resumeAll() {
        downloadManager.resumeAllAsync()
    }

    fun retry(id: Int) {
        downloadManager.retryAsync(id)
    }

    fun retry(tag: String) {
        downloadManager.retryAsync(tag)
    }

    fun retryAll() {
        downloadManager.retryAllAsync()
    }

    fun clearAllDb() {
        downloadManager.clearAllDbAsync()
    }

    fun clearDb(timeInMillis: Long) {
        downloadManager.clearDbAsync(timeInMillis)
    }

    fun clearDb(id: Int) {
        downloadManager.clearDbAsync(id)
    }

    fun clearDb(tag: String) {
        downloadManager.clearDbAsync(tag)
    }

    suspend fun isContentValid(
        url: String,
        headers: HashMap<String, String> = hashMapOf(),
        eTag: String
    ): Boolean {
        return ETagChecker(url, RetrofitInstance.getDownloadService()).getETag(headers) == eTag
    }

}
