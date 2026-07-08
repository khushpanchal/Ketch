package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.ketch.internal.database.DatabaseInstance
import com.ketch.internal.database.DownloadDao
import com.ketch.internal.database.DownloadEntity
import com.ketch.internal.download.DownloadManager
import io.mockk.coEvery
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PerformanceTest {

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
    fun `syncAndMap throttles disk checks`() = runBlocking {
        val id = 1
        val entity = DownloadEntity(
            id = id,
            path = context.cacheDir.absolutePath,
            fileName = "test.zip",
            status = Status.SUCCESS.name
        )

        coEvery { downloadDao.find(id) } returns entity
        
        val downloadManager = DownloadManager(
            context = context,
            downloadDao = downloadDao,
            workManager = workManager,
            downloadConfig = DownloadConfig(),
            notificationConfig = NotificationConfig(smallIcon = 1),
            logger = logger
        )

        // Multiple calls in quick succession
        downloadManager.getDownloadModelById(id)
        downloadManager.getDownloadModelById(id)
        downloadManager.getDownloadModelById(id)

        // Verify that disk check or database check for this entity was limited
        // Since syncAndMap is private, we verify that downloadDao.update (triggered by missing file) 
        // is NOT called 3 times if the file is missing but throttled.
        // Actually, let's verify that the check happens once.
        
        // Ensure file is missing
        File(entity.path, entity.fileName).delete()

        downloadManager.getDownloadModelById(id) // Check 1 - updates DB
        downloadManager.getDownloadModelById(id) // Check 2 - throttled
        downloadManager.getDownloadModelById(id) // Check 3 - throttled

        coVerify(exactly = 1) {
            downloadDao.update(any())
        }
    }
}
