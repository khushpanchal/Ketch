package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
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
import com.ketch.internal.utils.FileUtil
import com.ketch.internal.utils.WorkUtil.toJson
import com.ketch.internal.worker.DownloadWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ServerUpdateTest {

    private lateinit var context: Context
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val downloadDatabase = mockk<DownloadDatabase>()
    private val downloadService = mockk<DownloadService>()

    private val url = "https://example.com/file.zip"
    private val path = "/tmp"
    private val fileName = "file.zip"
    
    private val downloadRequest = DownloadRequest(
        url = url,
        path = path,
        fileName = fileName,
        tag = "test"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockkObject(DatabaseInstance)
        mockkObject(RetrofitInstance)
        mockkConstructor(ApiResponseHeaderChecker::class)
        mockkConstructor(DownloadTask::class)
        mockkObject(FileUtil)

        every { DatabaseInstance.getInstance(any()) } returns downloadDatabase
        every { downloadDatabase.downloadDao() } returns downloadDao
        every { RetrofitInstance.getDownloadService() } returns downloadService

        // Create the directory so File(path, fileName).exists() can be tested if needed
        File(path).mkdirs()
    }

    @After
    fun tearDown() {
        unmockkAll()
        File(path, fileName).delete()
    }

    @Test
    fun `worker restarts download if server Content-Length differs from database`() = runBlocking {
        val existingEntity = DownloadEntity(
            id = downloadRequest.id,
            url = url,
            path = path,
            fileName = fileName,
            totalBytes = 1000L, // Old size
            status = Status.SUCCESS.toString(),
            eTag = "old-etag"
        )

        coEvery { downloadDao.find(downloadRequest.id) } returns existingEntity
        
        // Mock server returning new size
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.CONTENT_LENGTH)
        } returns "2000" // New size
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.ETAG_HEADER)
        } returns "old-etag"

        // Mock DownloadTask to just return
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } returns 2000L

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to NotificationConfig(smallIcon = 1).toJson()
            )
        ).build()

        worker.doWork()

        // Verify that files were deleted due to size change
        coVerify {
            FileUtil.deleteFileIfExists(path, fileName)
            downloadDao.update(match { it.totalBytes == 2000L && it.downloadedBytes == 0L })
        }
    }

    @Test
    fun `worker restarts download if server ETag differs from database`() = runBlocking {
        val existingEntity = DownloadEntity(
            id = downloadRequest.id,
            url = url,
            path = path,
            fileName = fileName,
            totalBytes = 1000L,
            status = Status.SUCCESS.toString(),
            eTag = "old-etag"
        )

        coEvery { downloadDao.find(downloadRequest.id) } returns existingEntity
        
        // Mock server returning same size but NEW ETag
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.CONTENT_LENGTH)
        } returns "1000"
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.ETAG_HEADER)
        } returns "new-etag"

        // Mock DownloadTask to just return
        coEvery {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        } returns 1000L

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to NotificationConfig(smallIcon = 1).toJson()
            )
        ).build()

        worker.doWork()

        // Verify that files were deleted due to ETag change
        coVerify {
            FileUtil.deleteFileIfExists(path, fileName)
            downloadDao.update(match { it.eTag == "new-etag" && it.downloadedBytes == 0L })
        }
    }

    @Test
    fun `worker skips download if already SUCCESS and server headers match`() = runBlocking {
        // Create a dummy file to satisfy File.exists()
        val file = File(path, fileName)
        file.parentFile?.mkdirs()
        file.createNewFile()

        val existingEntity = DownloadEntity(
            id = downloadRequest.id,
            url = url,
            path = path,
            fileName = fileName,
            totalBytes = 1000L,
            status = Status.SUCCESS.toString(),
            eTag = "same-etag"
        )

        coEvery { downloadDao.find(downloadRequest.id) } returns existingEntity
        
        // Mock server returning same headers
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.CONTENT_LENGTH)
        } returns "1000"
        coEvery {
            anyConstructed<ApiResponseHeaderChecker>().getHeaderValue(DownloadConst.ETAG_HEADER)
        } returns "same-etag"

        val worker = TestListenableWorkerBuilder<DownloadWorker>(
            context = context,
            inputData = workDataOf(
                DownloadConst.KEY_DOWNLOAD_REQUEST to downloadRequest.toJson(),
                DownloadConst.KEY_NOTIFICATION_CONFIG to NotificationConfig(smallIcon = 1).toJson()
            )
        ).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())

        // Verify that DownloadTask was NOT called
        coVerify(exactly = 0) {
            anyConstructed<DownloadTask>().download(any(), any(), any())
        }
    }
}
