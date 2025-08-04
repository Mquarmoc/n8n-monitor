package com.example.n8nmonitor.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.settings.SettingsDataStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class MonitoringWorkerTest {

    private lateinit var context: Context
    private lateinit var repository: N8nRepository
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
        settingsDataStore = mockk(relaxed = true)
    }

    @Test
    fun testWorkerSuccess() = runBlocking {
        // Given
        val oneHourAgo = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)))
        
        // Mock settings
        coEvery { settingsDataStore.isNotificationEnabled } returns flowOf(true)
        
        // Mock repository responses
        coEvery { repository.getFailedExecutionsSince(any()) } returns emptyList()
        coEvery { repository.cleanupStaleData() } returns Unit
        
        // Build and run worker
        val worker = TestListenableWorkerBuilder<MonitoringWorker>(context)
            .setWorkerFactory(TestWorkerFactory(repository, settingsDataStore))
            .build()
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun testWorkerWithFailedExecutions() = runBlocking {
        // Given
        val failedExecutions = listOf(
            ExecutionEntity(
                id = "exec1",
                workflowId = "workflow1",
                status = "failed",
                startTime = System.currentTimeMillis() - 30 * 60 * 1000, // 30 minutes ago
                endTime = System.currentTimeMillis() - 29 * 60 * 1000,   // 29 minutes ago
                dataChunkPath = null
            )
        )
        
        // Mock settings
        coEvery { settingsDataStore.isNotificationEnabled } returns flowOf(true)
        
        // Mock repository responses
        coEvery { repository.getFailedExecutionsSince(any()) } returns failedExecutions
        coEvery { repository.cleanupStaleData() } returns Unit
        
        // Build and run worker
        val worker = TestListenableWorkerBuilder<MonitoringWorker>(context)
            .setWorkerFactory(TestWorkerFactory(repository, settingsDataStore))
            .build()
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun testWorkerWithNotificationsDisabled() = runBlocking {
        // Given
        // Mock settings with notifications disabled
        coEvery { settingsDataStore.isNotificationEnabled } returns flowOf(false)
        
        // Build and run worker
        val worker = TestListenableWorkerBuilder<MonitoringWorker>(context)
            .setWorkerFactory(TestWorkerFactory(repository, settingsDataStore))
            .build()
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(Result.success(), result)
    }

    @Test
    fun testWorkerWithException() = runBlocking {
        // Given
        // Mock settings
        coEvery { settingsDataStore.isNotificationEnabled } returns flowOf(true)
        
        // Mock repository to throw exception
        coEvery { repository.getFailedExecutionsSince(any()) } throws RuntimeException("Test exception")
        
        // Build and run worker
        val worker = TestListenableWorkerBuilder<MonitoringWorker>(context)
            .setWorkerFactory(TestWorkerFactory(repository, settingsDataStore))
            .build()
        
        // When
        val result = worker.doWork()
        
        // Then - Should retry on first attempt
        assertEquals(Result.retry(), result)
    }
}