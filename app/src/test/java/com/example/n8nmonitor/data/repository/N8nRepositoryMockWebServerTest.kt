package com.example.n8nmonitor.data.repository

import androidx.room.Room
import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.N8nDatabase
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.WorkflowDao
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import io.mockk.mockk

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class N8nRepositoryMockWebServerTest {

    // Removed InstantTaskExecutorRule as it's not needed for this test

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: N8nApiService
    private lateinit var appDatabase: N8nDatabase
    private lateinit var workflowDao: WorkflowDao
    private lateinit var executionDao: ExecutionDao
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: N8nRepository

    @Before
    fun setup() {
        // Setup MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Setup Moshi
        val moshi = Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()

        // Setup Retrofit with MockWebServer URL
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApiService::class.java)

        // Setup in-memory Room database
        appDatabase = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            N8nDatabase::class.java
        ).allowMainThreadQueries().build()

        workflowDao = appDatabase.workflowDao()
        executionDao = appDatabase.executionDao()
        settingsDataStore = mockk(relaxed = true)

        // Create repository with real dependencies
        repository = N8nRepository(apiService, workflowDao, executionDao, settingsDataStore, okHttpClient, moshi)
    }

    @After
    fun tearDown() {
        appDatabase.close()
        mockWebServer.shutdown()
    }

    @Test
    fun `getWorkflows returns cached data from database`() = runTest {
        // Given - Insert workflow into database
        val workflowId = "1"
        val workflowName = "MyWorkflow"
        val workflowEntity = com.example.n8nmonitor.data.database.WorkflowEntity(
            id = workflowId,
            name = workflowName,
            active = true,
            updatedAt = "2023-01-01T00:00:00Z",
            tags = null
        )
        workflowDao.insertWorkflows(listOf(workflowEntity))

        // When
        val result = repository.getWorkflows().first()

        // Then
        assertEquals(1, result.size)
        assertEquals(workflowName, result[0].name)
    }

    @Test
    fun `refreshWorkflows fetches from API and updates database`() = runTest {
        // Given - Prepare mock response
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                [
                  {
                    "id": "1",
                    "name": "API Workflow",
                    "active": true,
                    "updatedAt": "2023-01-01T00:00:00Z",
                    "tags": [],
                    "nodes": [],
                    "triggers": []
                  }
                ]
            """)
        mockWebServer.enqueue(mockResponse)

        // When
        val result = repository.refreshWorkflows()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("API Workflow", result.getOrNull()?.get(0)?.name)

        // Verify database was updated
        val dbWorkflows = workflowDao.getWorkflows().first()
        assertEquals(1, dbWorkflows.size)
        assertEquals("API Workflow", dbWorkflows[0].name)
    }

    @Test
    fun `refreshExecutions fetches from API and updates database`() = runTest {
        // Given - Prepare mock response
        val workflowId = "1"
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                {
                  "results": [
                    {
                      "id": "exec1",
                      "workflowId": "$workflowId",
                      "status": "success",
                      "start": "2023-01-01T00:00:00Z",
                      "end": "2023-01-01T00:01:00Z",
                      "nodes": [],
                      "timing": {
                        "start": "2023-01-01T00:00:00Z",
                        "end": "2023-01-01T00:01:00Z",
                        "duration": 60000
                      }
                    }
                  ],
                  "nextCursor": null
                }
            """)
        mockWebServer.enqueue(mockResponse)

        // When
        val result = repository.refreshExecutionsForWorkflow(workflowId, 10)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("exec1", result.getOrNull()?.get(0)?.id)

        // Verify database was updated
        val dbExecutions = executionDao.getExecutionsForWorkflow(workflowId).first()
        assertEquals(1, dbExecutions.size)
        assertEquals("exec1", dbExecutions[0].id)
    }

    @Test
    fun `stopExecution calls API with correct endpoint`() = runTest {
        // Given - Prepare mock response
        val executionId = "exec1"
        val mockResponse = MockResponse().setResponseCode(200)
        mockWebServer.enqueue(mockResponse)

        // When
        val result = repository.stopExecution(executionId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())

        // Verify the request
        val request = mockWebServer.takeRequest()
        assertEquals("/executions/$executionId/stop", request.path)
        assertEquals("POST", request.method)
    }

    @Test
    fun `getFailedExecutionsSince returns executions from database`() = runTest {
        // Given - Insert failed executions into database
        val workflowId = "1"
        val executionId = "exec1"
        val timestamp = "2023-01-01T00:00:00.000Z"
        
        val execution = com.example.n8nmonitor.data.database.ExecutionEntity(
            id = executionId,
            workflowId = workflowId,
            status = "failed",
            startTime = "2023-01-01T00:00:00Z",
            endTime = "2023-01-01T00:01:00Z",
            duration = 60000L
        )
        executionDao.insertExecutions(listOf(execution))

        // When
        val result = repository.getFailedExecutionsSince(timestamp)

        // Then
        assertEquals(1, result.size)
        assertEquals(executionId, result[0].id)
        assertEquals("failed", result[0].status)
    }
}