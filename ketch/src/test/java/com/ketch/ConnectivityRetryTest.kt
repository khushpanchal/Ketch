package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.download.DownloadManager
import com.ketch.internal.download.DownloadRequest
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectivityRetryTest {

    private lateinit var context: Context
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    @Before
    fun setUp() {
        val field = Ketch::class.java.getDeclaredField("ketchInstance")
        field.isAccessible = true
        field.set(null, null)

        context = ApplicationProvider.getApplicationContext()
        mockkObject(DatabaseInstance)
        every { DatabaseInstance.getInstance(any()).downloadDao() } returns downloadDao
        
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosByTagFlow(any()) } returns emptyFlow()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `default work request has CONNECTED constraint`() = runBlocking {
        val downloadConfig = DownloadConfig()
        val downloadManager = DownloadManager(
            context = context,
            downloadDao = downloadDao,
            workManager = workManager,
            downloadConfig = downloadConfig,
            notificationConfig = NotificationConfig(smallIcon = 1),
            logger = logger
        )

        val request = DownloadRequest(
            url = "https://example.com",
            path = "/path",
            fileName = "file.zip",
            tag = "tag"
        )

        downloadManager.download(request)

        coVerify {
            workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), match<OneTimeWorkRequest> {
                it.workSpec.constraints.requiredNetworkType == NetworkType.CONNECTED
            })
        }
    }
}
