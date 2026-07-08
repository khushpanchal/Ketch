package com.ketch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.common.truth.Truth.assertThat
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileSyncTest {

    private lateinit var context: Context
    private val downloadDao = mockk<DownloadDao>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    @Before
    fun setUp() {
        // Reset Ketch singleton
        val field = Ketch::class.java.getDeclaredField("ketchInstance")
        field.isAccessible = true
        field.set(null, null)

        context = ApplicationProvider.getApplicationContext()
        mockkObject(DatabaseInstance)
        every { DatabaseInstance.getInstance(any()).downloadDao() } returns downloadDao
        
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        every { workManager.getWorkInfosByTagFlow(any()) } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Ketch initialization triggers sync and updates FAILED if SUCCESS file missing`() = runBlocking {
        val url = "https://example.com/file.zip"
        val path = context.cacheDir.absolutePath
        val fileName = "missing.zip"
        val id = 1

        val entity = DownloadEntity(
            id = id,
            url = url,
            path = path,
            fileName = fileName,
            status = Status.SUCCESS.name
        )

        // Ensure file does NOT exist
        File(path, fileName).delete()

        coEvery { downloadDao.getAllEntity() } returns listOf(entity)

        // Initialize Ketch (this triggers DownloadManager init which calls syncDbWithDisk)
        Ketch.builder().build(context)

        // Give it a moment to run the async sync
        kotlinx.coroutines.delay(500)

        coVerify {
            downloadDao.update(match { it.id == id && it.status == Status.FAILED.name && it.failureReason == "File missing from disk" })
        }
    }

    @Test
    fun `querying download status triggers sync and updates FAILED if SUCCESS file missing`() = runBlocking {
        val url = "https://example.com/file2.zip"
        val path = context.cacheDir.absolutePath
        val fileName = "missing2.zip"
        val id = 2

        val entity = DownloadEntity(
            id = id,
            url = url,
            path = path,
            fileName = fileName,
            status = Status.SUCCESS.name
        )

        // Ensure file does NOT exist
        File(path, fileName).delete()

        coEvery { downloadDao.find(id) } returns entity
        // To satisfy syncAndMap which returns DownloadModel, it calls toDownloadModel which calls jsonToHashMap
        // But headersJson is empty by default so it should be fine.

        val downloadManager = DownloadManager(
            context = context,
            downloadDao = downloadDao,
            workManager = workManager,
            downloadConfig = DownloadConfig(),
            notificationConfig = NotificationConfig(smallIcon = 1),
            logger = logger
        )

        // Query status
        val model = downloadManager.getDownloadModelById(id)

        assertThat(model?.status).isEqualTo(Status.FAILED)
        
        coVerify {
            downloadDao.update(match { it.id == id && it.status == Status.FAILED.name })
        }
    }

    @Test
    fun `observing downloads triggers sync and updates FAILED if SUCCESS file missing`() = runBlocking {
        val url = "https://example.com/file3.zip"
        val path = context.cacheDir.absolutePath
        val fileName = "missing3.zip"
        val id = 3

        val entity = DownloadEntity(
            id = id,
            url = url,
            path = path,
            fileName = fileName,
            status = Status.SUCCESS.name
        )

        // Ensure file does NOT exist
        File(path, fileName).delete()

        val entityFlow = MutableStateFlow(listOf(entity))
        coEvery { downloadDao.getAllEntityFlow() } returns entityFlow

        val downloadManager = DownloadManager(
            context = context,
            downloadDao = downloadDao,
            workManager = workManager,
            downloadConfig = DownloadConfig(),
            notificationConfig = NotificationConfig(smallIcon = 1),
            logger = logger
        )

        // Observe
        val models = downloadManager.observeAllDownloads().first()

        assertThat(models.first().status).isEqualTo(Status.FAILED)
        
        coVerify {
            downloadDao.update(match { it.id == id && it.status == Status.FAILED.name })
        }
    }
}
