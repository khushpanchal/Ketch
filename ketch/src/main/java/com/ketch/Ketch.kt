package com.ketch

import android.content.Context
import androidx.work.WorkManager
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.download.ApiResponseHeaderChecker
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.DownloadLogger
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.NotificationConst
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Ketch: Core singleton class client interacts with
 *
 * How to initialize [Ketch] instance?
 *
 * ```
 * // Simplest way to initialize:
 * Ketch.builder().build(context)
 *
 * // Sample to initialize the library inside application class
 * class MainApplication : Application() {
 *
 *     lateinit var ketch: Ketch
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         ketch = Ketch.builder()
 *             .setOkHttpClient(...) // optional
 *             .setDownloadConfig(DownloadConfig()) // optional
 *             .setNotificationConfig( // optional, Notification is off by default
 *                 NotificationConfig(
 *                     true,
 *                     smallIcon = R.drawable.ic_launcher_foreground
 *                 )
 *             )
 *             .enableLogs(true) // optional, logs are off by default
 *             .setLogger(...) // optional, pass your own logger implementation
 *             .build(this)
 *     }
 *
 * }
 *
 * // To use the library
 * ketch.download(url, path, fileName) // download
 * ketch.pause(id) // pause download
 * ketch.resume(id) // resume download
 * ketch.retry(id) // retry download
 * ketch.cancel(id) // cancel download
 * ketch.clearDb(id) // clear database and delete file
 *
 * // To observe the downloads
 * lifecycleScope.launch {
 *    repeatOnLifecycle(Lifecycle.State.STARTED) {
 *       ketch.observeDownloads()
 *        .flowOn(Dispatchers.IO)
 *        .collect { downloadModelList ->
 *         // take appropriate action with observed list of [DownloadModel]
 *       }
 *    }
 * }
 *
 * ```
 *
 * JOURNEY OF SINGLE DOWNLOAD FILE:
 *
 * [Status.QUEUED] -> [Status.STARTED] -> [Status.PROGRESS] -> Download in progress
 * Terminating states: [Status.PAUSED], [Status.CANCELLED], [Status.FAILED], [Status.SUCCESS]
 *
 * @property context Application context
 * @property downloadConfig [DownloadConfig] to configure download related info
 * @property notificationConfig [NotificationConfig] to configure notification related info
 * @property logger [Logger] implementation to print logs
 * @constructor Create empty Ketch
 */
@Suppress("TooManyFunctions")
class Ketch private constructor(
    private val context: Context,
    private var downloadConfig: DownloadConfig,
    private var notificationConfig: NotificationConfig,
    private var logger: Logger,
    private var okHttpClient: OkHttpClient
) {

    private val mutex = Mutex()

    companion object {

        @Volatile
        private var ketchInstance: Ketch? = null

        fun builder() = Builder()

        class Builder {
            private var downloadConfig: DownloadConfig = DownloadConfig()
            private var notificationConfig: NotificationConfig = NotificationConfig(
                smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON
            )
            private var logger: Logger = DownloadLogger(false)
            private lateinit var okHttpClient: OkHttpClient

            /**
             * Set download config: It has no effect if using [setOkHttpClient] function
             * Pass timeout values inside okHttpClient itself
             *
             * @param config [DownloadConfig]
             */
            fun setDownloadConfig(config: DownloadConfig) = apply {
                this.downloadConfig = config
            }

            fun setNotificationConfig(config: NotificationConfig) = apply {
                this.notificationConfig = config
            }

            fun enableLogs(enable: Boolean) = apply {
                this.logger = DownloadLogger(enable)
            }

            fun setLogger(logger: Logger) = apply {
                this.logger = logger
            }

            fun setOkHttpClient(okHttpClient: OkHttpClient) = apply {
                this.okHttpClient = okHttpClient
            }

            @Synchronized
            fun build(context: Context): Ketch {
                if (ketchInstance == null) {

                    if (!::okHttpClient.isInitialized) {
                        okHttpClient = OkHttpClient
                            .Builder()
                            .connectTimeout(downloadConfig.connectTimeOutInMs, TimeUnit.MILLISECONDS)
                            .readTimeout(downloadConfig.readTimeOutInMs, TimeUnit.MILLISECONDS)
                            .build()
                    }

                    ketchInstance = Ketch(
                        context = context.applicationContext,
                        downloadConfig = downloadConfig,
                        notificationConfig = notificationConfig,
                        logger = logger,
                        okHttpClient = okHttpClient
                    )
                }
                return ketchInstance!!
            }
        }
    }

    init {
        RetrofitInstance.getDownloadService(okHttpClient = okHttpClient)
    }

    private val downloadManager = DownloadManager(
        context = context,
        downloadDao = DatabaseInstance.getInstance(context).downloadDao(),
        workManager = WorkManager.getInstance(context.applicationContext),
        downloadConfig = downloadConfig,
        notificationConfig = notificationConfig,
        logger = logger
    )

    /**
     * Download the content
     *
     * @param url Download url of the content
     * @param path Download path to store the downloaded file
     * @param fileName Name of the file to be downloaded
     * @param tag Optional tag for each download to group the download into category
     * @param metaData Optional metaData set for adding any extra download info
     * @param headers Optional headers sent when making api call for file download
     * @param supportPauseResume Optional flag to enable pause and resume functionality
     * @return Unique Download ID associated with current download
     */
    fun download(
        url: String,
        path: String,
        fileName: String = FileUtil.getFileNameFromUrl(url),
        tag: String = "",
        metaData: String = "",
        headers: HashMap<String, String> = hashMapOf(),
        supportPauseResume: Boolean = true,
    ): Int {
        val downloadRequest = prepareDownloadRequest(
            url = url,
            path = path,
            fileName = fileName,
            tag = tag,
            headers = headers,
            metaData = metaData,
            supportPauseResume = supportPauseResume,
        )
        downloadManager.downloadAsync(downloadRequest)
        return downloadRequest.id
    }

    /**
     * Download the content synchronously, it will return the download id once download is entered in the queue
     *
     * @param url Download url of the content
     * @param path Download path to store the downloaded file
     * @param fileName Name of the file to be downloaded
     * @param tag Optional tag for each download to group the download into category
     * @param metaData Optional metaData set for adding any extra download info
     * @param headers Optional headers sent when making api call for file download
     * @return Unique Download ID associated with current download
     */
    suspend fun downloadSync(
        url: String,
        path: String,
        fileName: String = FileUtil.getFileNameFromUrl(url),
        tag: String = "",
        metaData: String = "",
        headers: HashMap<String, String> = hashMapOf(),
        supportPauseResume: Boolean = true,
    ): Int {
        val downloadRequest = mutex.withLock {
            prepareDownloadRequest(
                url = url,
                path = path,
                fileName = fileName,
                tag = tag,
                headers = headers,
                metaData = metaData,
                supportPauseResume = supportPauseResume,
            )
        }
        downloadManager.download(downloadRequest)
        return downloadRequest.id
    }

    /**
     * Cancel download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun cancel(id: Int) {
        downloadManager.cancelAsync(id)
    }

    /**
     * Cancel downloads with given [tag]
     *
     * @param tag Tag associated with the download
     */
    fun cancel(tag: String) {
        downloadManager.cancelAsync(tag)
    }

    /**
     * Cancel all the downloads
     *
     */
    fun cancelAll() {
        downloadManager.cancelAllAsync()
    }

    /**
     * Pause download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun pause(id: Int) {
        downloadManager.pauseAsync(id)
    }

    /**
     * Pause downloads with given [tag]
     *
     * @param tag Tag associated with the download
     */
    fun pause(tag: String) {
        downloadManager.pauseAsync(tag)
    }

    /**
     * Pause all the downloads
     *
     */
    fun pauseAll() {
        downloadManager.pauseAllAsync()
    }

    /**
     * Resume download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun resume(id: Int) {
        downloadManager.resumeAsync(id)
    }

    /**
     * Resume downloads with given [tag]
     *
     * @param tag Tag associated with the download
     */
    fun resume(tag: String) {
        downloadManager.resumeAsync(tag)
    }

    /**
     * Resume all the downloads
     *
     */
    fun resumeAll() {
        downloadManager.resumeAllAsync()
    }

    /**
     * Retry download with given [id]
     *
     * @param id Unique Download ID of the download
     */
    fun retry(id: Int) {
        downloadManager.retryAsync(id)
    }

    /**
     * Retry downloads with given [tag]
     *
     * @param tag Tag associated with the download
     */
    fun retry(tag: String) {
        downloadManager.retryAsync(tag)
    }

    /**
     * Retry all the downloads
     *
     */
    fun retryAll() {
        downloadManager.retryAllAsync()
    }

    /**
     * Clear all entries from database and delete all the files
     *
     * @param deleteFile delete the actual file from the system
     */
    fun clearAllDb(deleteFile: Boolean = true) {
        downloadManager.clearAllDbAsync(deleteFile)
    }

    /**
     * Clear entries from database and delete files on or before [timeInMillis]
     *
     * @param timeInMillis timestamp in millisecond
     * @param deleteFile delete the actual file from the system
     */
    fun clearDb(timeInMillis: Long, deleteFile: Boolean = true) {
        downloadManager.clearDbAsync(timeInMillis, deleteFile)
    }

    /**
     * Clear entry from database and delete file with given [id]
     *
     * @param id Unique Download ID of the download
     * @param deleteFile delete the actual file from the system
     */
    fun clearDb(id: Int, deleteFile: Boolean = true) {
        downloadManager.clearDbAsync(id, deleteFile)
    }

    /**
     * Clear entries from database and delete files with given [tag]
     *
     * @param tag Tag associated with the download
     * @param deleteFile delete the actual file from the system
     */
    fun clearDb(tag: String, deleteFile: Boolean = true) {
        downloadManager.clearDbAsync(tag, deleteFile)
    }

    /**
     * Suspend function to make headers only api call to get and compare ETag string of content
     *
     * @param url Download Url
     * @param headers Optional headers associated with url of download request
     * @param eTag Existing ETag of content
     * @return Boolean to compare existing and newly fetched ETag of the content
     */
    suspend fun isContentValid(
        url: String,
        headers: HashMap<String, String> = hashMapOf(),
        eTag: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            ApiResponseHeaderChecker(url, RetrofitInstance.getDownloadService(), headers)
                .getHeaderValue(DownloadConst.ETAG_HEADER) == eTag
        }

    /**
     * Suspend function to make headers only api call to get length of content in bytes
     *
     * @param url Download Url
     * @param headers Optional headers associated with url of download request
     * @return Length of content to be downloaded in bytes
     */
    suspend fun getContentLength(
        url: String,
        headers: HashMap<String, String> = hashMapOf()
    ): Long =
        withContext(Dispatchers.IO) {
            ApiResponseHeaderChecker(url, RetrofitInstance.getDownloadService(), headers)
                .getHeaderValue(DownloadConst.CONTENT_LENGTH)?.toLong() ?: 0
        }

    /**
     * Observe all downloads
     *
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloads(): Flow<List<DownloadModel>> {
        return downloadManager.observeAllDownloads()
    }

    /**
     * Observe download with given [id]
     *
     * @param id Unique Download ID of the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadById(id: Int): Flow<DownloadModel?> {
        return downloadManager.observeDownloadById(id)
    }

    /**
     * Observe downloads with given [tag]
     *
     * @param tag Tag associated with the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadByTag(tag: String): Flow<List<DownloadModel>> {
        return downloadManager.observeDownloadsByTag(tag)
    }

    /**
     * Observe downloads with given [status]
     *
     * @param status Status associated with the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadsByStatus(status: Status): Flow<List<DownloadModel>> {
        return downloadManager.observeDownloadsByStatus(status)
    }

    /**
     * Observe downloads with given [ids]
     *
     * @param ids List of ids associated with the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadsByIds(ids: List<Int>): Flow<List<DownloadModel?>> {
        return downloadManager.observeDownloadsByIds(ids)
    }

    /**
     * Observe downloads with given [tags]
     *
     * @param tags List of tags associated with the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadsByTags(tags: List<String>): Flow<List<DownloadModel>> {
        return downloadManager.observeDownloadsByTags(tags)
    }

    /**
     * Observe downloads with given [statuses]
     *
     * @param statuses List of statuses associated with the download
     * @return [Flow] of List of [DownloadModel]
     */
    fun observeDownloadsByStatuses(statuses: List<Status>): Flow<List<DownloadModel>> {
        return downloadManager.observeDownloadsByStatuses(statuses)
    }

    /**
     * Suspend function to get list of all Downloads
     *
     * @return List of [DownloadModel]
     */
    suspend fun getAllDownloads() = downloadManager.getAllDownloads()

    /**
     * Suspend function to get download model by id
     *
     * @param id
     * @return [DownloadModel] if present else null
     */
    suspend fun getDownloadModelById(id: Int) = downloadManager.getDownloadModelById(id)

    /**
     * Suspend function to get download model by tag
     *
     * @param tag
     * @return [DownloadModel] if present else null
     */
    suspend fun getDownloadModelByTag(tag: String) = downloadManager.getDownloadModelByTag(tag)

    /**
     * Suspend function to get download model by status
     *
     * @param status
     * @return [DownloadModel] if present else null
     */
    suspend fun getDownloadModelByStatus(status: Status) = downloadManager.getDownloadModelByStatus(status)

    /**
     * Suspend function to get download model by list of tags
     *
     * @param tags
     * @return List of [DownloadModel]
     */
    suspend fun getDownloadModelByTags(tags: List<String>) = downloadManager.getDownloadModelByTags(tags)

    /**
     * Suspend function to get download model by list of ids
     *
     * @param ids
     * @return List of [DownloadModel]
     */
    suspend fun getDownloadModelByIds(ids: List<Int>) = downloadManager.getDownloadModelByIds(ids)

    /**
     * Suspend function to get download model by list of statuses
     *
     * @param statuses
     * @return List of [DownloadModel]
     */
    suspend fun getDownloadModelByStatuses(statuses: List<Status>) = downloadManager.getDownloadModelByStatuses(statuses)

    private fun prepareDownloadRequest(
        url: String,
        path: String,
        fileName: String,
        tag: String,
        headers: HashMap<String, String>,
        metaData: String,
        supportPauseResume: Boolean,
    ): DownloadRequest {
        require(url.isNotEmpty() && path.isNotEmpty() && fileName.isNotEmpty()) {
            "Missing ${if (url.isEmpty()) "url" else if (path.isEmpty()) "path" else "fileName"}"
        }

        // This will create a temp file which will be renamed after successful download.
        // This will also make sure each file name is unique.
        val newFileName = FileUtil.resolveNamingConflicts(fileName, path)
        FileUtil.createTempFileIfNotExists(path, newFileName)

        val downloadRequest = DownloadRequest(
            url = url,
            path = path,
            fileName = newFileName,
            tag = tag,
            headers = headers,
            metaData = metaData,
            supportPauseResume = supportPauseResume,
        )

        return downloadRequest
    }

}
