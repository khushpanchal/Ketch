package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.utils.UserAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeduplicationRetryTest {

    private lateinit var context: Context
    private lateinit var ketch: Ketch
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    @Before
    fun setUp() {
        // Reset Ketch singleton
        val field = Ketch::class.java.getDeclaredField("ketchInstance")
        field.isAccessible = true
        field.set(null, null)

        context = ApplicationProvider.getApplicationContext()
        // We use a mock WorkManager instead of WorkManagerTestInitHelper to verify enqueues
        mockkObject(DatabaseInstance)
        every { DatabaseInstance.getInstance(any()).downloadDao() } returns downloadDao
        
        // Mock WorkManager.getInstance(context)
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager

        ketch = Ketch.builder().build(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `retryAll only retries one entry if multiple exist for same file`() = runBlocking {
        val url = "https://example.com/file.zip"
        val path = "/downloads"
        
        val entity1 = DownloadEntity(
            id = 1,
            url = url,
            path = path,
            fileName = "file.zip",
            status = Status.FAILED.toString()
        )
        val entity2 = DownloadEntity(
            id = 2,
            url = url,
            path = path,
            fileName = "file (1).zip",
            status = Status.FAILED.toString()
        )

        val allEntities = listOf(entity1, entity2)
        coEvery { downloadDao.getAllEntity() } returns allEntities
        coEvery { downloadDao.find(1) } returns entity1
        coEvery { downloadDao.find(2) } returns entity2
        
        // Mock countOtherActiveOrSuccessful
        // When checking for entity1, entity2 is FAILED (not active)
        coEvery { downloadDao.countOtherActiveOrSuccessful(url, path, 1, any()) } returns 0
        // When checking for entity2, entity1 will be updated to QUEUED (active) by the time entity2 is processed
        coEvery { downloadDao.countOtherActiveOrSuccessful(url, path, 2, any()) } answers {
            // Simulate that entity1 is now active
            1 
        }

        ketch.retryAll()
        
        // Give it a moment to process the async calls
        kotlinx.coroutines.delay(500)

        // Verify that enqueueUniqueWork was called ONLY for one of them (likely the first one)
        // Since retryAsync is launched in a loop, order might vary, but only one should succeed
        coVerify(exactly = 1) {
            workManager.enqueueUniqueWork(any(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        }
    }
}
