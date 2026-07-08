package com.ketch.internal.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.ketch.DownloadConfig
import com.ketch.NotificationConfig
import com.ketch.Status
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.database.DownloadDatabase
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.download.ApiResponseHeaderChecker
import com.ketch.internal.download.DownloadRequest
import com.ketch.internal.download.DownloadTask
import com.ketch.internal.network.DownloadService
import com.ketch.internal.network.RetrofitInstance
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.NotificationConst
import com.ketch.internal.utils.UserAction
import com.ketch.internal.utils.WorkUtil.toJson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadWorkerTest {

    private lateinit var context: Context
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val downloadDatabase = mockk<DownloadDatabase>()
    private val downloadService = mockk<DownloadService>()

    private val downloadRequest = DownloadRequest(
        url = "https://example.com/file.zip",
        path = "/tmp",
        fileName = "file.zip",
        tag = "test"
    )

    private val downloadEntity = DownloadEntity(
        id = downloadRequest.id,
        url = downloadRequest.url,
        path = downloadRequest.path,
        fileName = downloadRequest.fileName,
        tag = downloadRequest.tag
    )

    private val notificationConfig = NotificationConfig(
        smallIcon = NotificationConst.DEFAULT_VALUE_NOTIFICATION_SMALL_ICON
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DatabaseInstance)
        mockkObject(RetrofitInstance)
        mockkConstructor(ApiResponseHeaderChecker::class)
        mockkConstructor(DownloadTask::class)

        every { DatabaseInstance.getInstance(any()) } returns downloadDatabase
        every { downloadDatabase.downloadDao() } returns downloadDao
        every { RetrofitInstance.getDownloadService() } returns downloadService

        coEvery { downloadDao.find(downloadRequest.id) } returns downloadEntity

        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(any())
        } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork on success sets statuses STARTED, PROGRESS, SUCCESS`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } coAnswers {
            val onStart = secondArg<suspend (Long) -> Unit>()
            val onProgress = thirdArg<suspend (Long, Long, Float) -> Unit>()

            onStart.invoke(1000L)
            onProgress.invoke(500L, 1000L, 10f)
            1000L
        }

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        coVerify {
            downloadDao.update(match { it.status == Status.STARTED.toString() })
            downloadDao.update(match { it.status == Status.PROGRESS.toString() })
            downloadDao.update(match { it.status == Status.SUCCESS.toString() })
        }
    }

    @Test
    fun `doWork on failure sets status FAILED`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws RuntimeException("Download failed")

        val downloadConfig = DownloadConfig(autoRetry = false)

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson(),
                DownloadConst.KEY_DOWNLOAD_CONFIG to downloadConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

        coVerify(timeout = 2000) {
            downloadDao.update(match { it.status == Status.FAILED.toString() && it.failureReason == "Download failed" })
        }
    }

    @Test
    fun `doWork on pause sets status PAUSED`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws CancellationException()

        // Mock user action as PAUSE
        coEvery { downloadDao.find(downloadRequest.id) } returns downloadEntity.copy(userAction = UserAction.PAUSE.toString())

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

        coVerify(timeout = 2000) {
            downloadDao.update(match { it.status == Status.PAUSED.toString() })
        }
    }

    @Test
    fun `doWork on cancel sets status CANCELLED`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws CancellationException()

        // Mock user action as CANCEL (anything other than PAUSE in current impl)
        coEvery { downloadDao.find(downloadRequest.id) } returns downloadEntity.copy(userAction = UserAction.CANCEL.toString())

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

        coVerify(timeout = 2000) {
            downloadDao.update(match { it.status == Status.CANCELLED.toString() })
        }
    }

    @Test
    fun `doWork on IOException with autoRetry sets status RETRY_QUEUED and returns retry`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws java.io.IOException("Network error")

        val downloadConfig = DownloadConfig(autoRetry = true)

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson(),
                DownloadConst.KEY_DOWNLOAD_CONFIG to downloadConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())

        coVerify(timeout = 2000) {
            downloadDao.update(match {
                it.status == Status.RETRY_QUEUED.toString() && it.failureReason == "Network error"
            })
        }
    }

    @Test
    fun `doWork with default config retries on IOException`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws java.io.IOException("Default retry error")

        // No DownloadConst.KEY_DOWNLOAD_CONFIG provided
        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())

        coVerify(timeout = 2000) {
            downloadDao.update(match {
                it.status == Status.RETRY_QUEUED.toString() && it.failureReason == "Default retry error"
            })
        }
    }

    @Test
    fun `doWork with autoRetry false fails on IOException`() = runBlocking {
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } throws java.io.IOException("No retry error")

        val downloadConfig = DownloadConfig(autoRetry = false)

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to notificationConfig.toJson(),
                DownloadConst.KEY_DOWNLOAD_CONFIG to downloadConfig.toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

        coVerify(timeout = 2000) {
            downloadDao.update(match {
                it.status == Status.FAILED.toString() && it.failureReason == "No retry error"
            })
        }
    }
}
