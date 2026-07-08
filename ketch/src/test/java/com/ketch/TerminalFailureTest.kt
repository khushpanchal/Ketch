package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.utils.DownloadConst
import com.ketch.internal.utils.UserAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TerminalFailureTest {

    private lateinit var context: Context
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val workInfoFlow = MutableStateFlow<List<WorkInfo>>(emptyList())

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(StandardTestDispatcher())
        every { workManager.getWorkInfosByTagFlow(DownloadConst.TAG_DOWNLOAD) } returns workInfoFlow
    }

    @Test
    fun `when WorkManager fails DownloadManager updates DB to FAILED and retries if eligible`() = runBlocking {
        val downloadId = 123
        val uuid = UUID.randomUUID()
        val downloadEntity = DownloadEntity(
            id = downloadId,
            url = "https://example.com",
            path = "/path",
            fileName = "file.zip",
            uuid = uuid.toString(),
            userAction = UserAction.START.toString()
        )

        coEvery { downloadDao.getAllEntity() } returns listOf(downloadEntity)
        coEvery { downloadDao.find(downloadId) } returns downloadEntity

        val downloadConfig = DownloadConfig(autoRetry = true)
        val notificationConfig = NotificationConfig(smallIcon = 1)

        val downloadManager = DownloadManager(
            context = context,
            downloadDao = downloadDao,
            workManager = workManager,
            downloadConfig = downloadConfig,
            notificationConfig = notificationConfig,
            logger = logger
        )

        // Simulate WorkManager failure
        val workInfo = mockk<WorkInfo> {
            every { id } returns uuid
            every { state } returns WorkInfo.State.FAILED
            every { runAttemptCount } returns 3
            every { progress } returns androidx.work.Data.EMPTY
        }

        workInfoFlow.value = listOf(workInfo)
        
        // Give some time for the collector in DownloadManager to process
        delay(500)

        coVerify {
            downloadDao.update(match { it.id == downloadId && it.status == Status.FAILED.toString() })
        }
        
        // Since autoRetry is true and userAction is START, it should trigger a retry
        // retry eventually calls downloadManager.download() which calls workManager.enqueueUniqueWork()
        coVerify(timeout = 2000) {
            workManager.enqueueUniqueWork(
                downloadId.toString(),
                any<ExistingWorkPolicy>(),
                any<OneTimeWorkRequest>()
            )
        }
    }
}
