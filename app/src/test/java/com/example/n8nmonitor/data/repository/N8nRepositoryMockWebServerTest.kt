package com.example.n8nmonitor.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.AppDatabase
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.WorkflowDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class N8nRepositoryMockWebServerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: N8nApiService
    private lateinit var appDatabase: AppDatabase
    private lateinit var workflowDao: WorkflowDao
    private lateinit var executionDao: ExecutionDao
    private lateinit var repository: N8nRepository

    @Before
    fun setup() {
        // Setup MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Setup Moshi
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
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
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        workflowDao = appDatabase.workflowDao()
        executionDao = appDatabase.executionDao()

        // Create repository with real dependencies
        repository = N8nRepository(apiService, workflowDao, executionDao)
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
            lastExecutionStatus = "success",
            lastExecutionTime = System.currentTimeMillis(),
            isBookmarked = false,
            lastSyncTime = System.currentTimeMillis()
        )
        workflowDao.insertWorkflows(listOf(workflowEntity))

        // When
        val result = repository.getWorkflows()

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
                    "nodes": [],
                    "connections": {},
                    "settings": {},
                    "staticData": null,
                    "tags": [],
                    "triggerCount": 0,
                    "updatedAt": "2023-01-01T00:00:00Z",
                    "versionId": "v1"
                  }
                ]
            """)
        mockWebServer.enqueue(mockResponse)

        // When
        val result = repository.refreshWorkflows()

        // Then
        assertEquals(1, result.size)
        assertEquals("API Workflow", result[0].name)

        // Verify database was updated
        val dbWorkflows = workflowDao.getAllWorkflows()
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
                  "data": [
                    {
                      "id": "exec1",
                      "workflowId": "$workflowId",
                      "status": "success",
                      "startTime": "2023-01-01T00:00:00Z",
                      "endTime": "2023-01-01T00:01:00Z",
                      "data": {},
                      "nodes": [],
                      "timing": null
                    }
                  ],
                  "meta": null
                }
            """)
        mockWebServer.enqueue(mockResponse)

        // When
        val result = repository.refreshExecutions(workflowId)

        // Then
        assertEquals(1, result.size)
        assertEquals("exec1", result[0].id)

        // Verify database was updated
        val dbExecutions = executionDao.getExecutionsForWorkflow(workflowId)
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
        assertTrue(result)

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
            startTime = System.currentTimeMillis() - 3600000, // 1 hour ago
            endTime = System.currentTimeMillis() - 3500000,   // 58 minutes ago
            dataChunkPath = null
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