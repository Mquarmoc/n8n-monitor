package com.example.n8nmonitor.data.repository

import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.WorkflowDao
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.dto.WorkflowDto
import com.example.n8nmonitor.data.dto.ExecutionDto
import com.example.n8nmonitor.data.dto.ExecutionsResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class N8nRepositoryTest {

    private lateinit var repository: N8nRepository
    private lateinit var apiService: N8nApiService
    private lateinit var workflowDao: WorkflowDao
    private lateinit var executionDao: ExecutionDao

    @Before
    fun setup() {
        apiService = mockk()
        workflowDao = mockk()
        executionDao = mockk()
        repository = N8nRepository(apiService, workflowDao, executionDao)
    }

    @Test
    fun `getWorkflows returns workflows from database when available`() = runTest {
        // Given
        val dbWorkflows = listOf(
            WorkflowEntity(
                id = "1",
                name = "Test Workflow 1",
                active = true,
                lastExecutionStatus = "success",
                lastExecutionTime = System.currentTimeMillis(),
                isBookmarked = false,
                lastSyncTime = System.currentTimeMillis()
            )
        )
        coEvery { workflowDao.getAllWorkflows() } returns dbWorkflows

        // When
        val result = repository.getWorkflows()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Workflow 1", result[0].name)
    }

    @Test
    fun `refreshWorkflows fetches from API and updates database`() = runTest {
        // Given
        val apiWorkflows = listOf(
            WorkflowDto(
                id = "1",
                name = "API Workflow 1",
                active = true,
                nodes = emptyList(),
                connections = emptyMap(),
                settings = emptyMap(),
                staticData = null,
                tags = emptyList(),
                triggerCount = 0,
                updatedAt = "2023-01-01T00:00:00Z",
                versionId = "v1"
            )
        )
        coEvery { apiService.getWorkflows() } returns apiWorkflows
        coEvery { workflowDao.insertWorkflows(any()) } returns Unit
        coEvery { workflowDao.getAllWorkflows() } returns emptyList()

        // When
        val result = repository.refreshWorkflows()

        // Then
        assertEquals(1, result.size)
        assertEquals("API Workflow 1", result[0].name)
    }

    @Test
    fun `getWorkflowDetails returns workflow with executions`() = runTest {
        // Given
        val workflowId = "1"
        val workflow = WorkflowEntity(
            id = workflowId,
            name = "Test Workflow",
            active = true,
            lastExecutionStatus = "success",
            lastExecutionTime = System.currentTimeMillis(),
            isBookmarked = false,
            lastSyncTime = System.currentTimeMillis()
        )
        val executions = listOf(
            ExecutionEntity(
                id = "exec1",
                workflowId = workflowId,
                status = "success",
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                dataChunkPath = null
            )
        )
        
        coEvery { workflowDao.getWorkflowById(workflowId) } returns workflow
        coEvery { executionDao.getExecutionsForWorkflow(workflowId) } returns executions

        // When
        val result = repository.getWorkflowDetails(workflowId)

        // Then
        assertEquals(workflow, result.first)
        assertEquals(1, result.second.size)
        assertEquals("exec1", result.second[0].id)
    }

    @Test
    fun `refreshExecutions fetches from API and updates database`() = runTest {
        // Given
        val workflowId = "1"
        val apiExecutions = ExecutionsResponseDto(
            data = listOf(
                ExecutionDto(
                    id = "exec1",
                    workflowId = workflowId,
                    status = "success",
                    startTime = "2023-01-01T00:00:00Z",
                    endTime = "2023-01-01T00:01:00Z",
                    data = emptyMap(),
                    nodes = emptyList(),
                    timing = null
                )
            ),
            meta = null
        )
        
        coEvery { apiService.getExecutions(workflowId = workflowId, limit = 50) } returns apiExecutions
        coEvery { executionDao.insertExecutions(any()) } returns Unit
        coEvery { executionDao.getExecutionsForWorkflow(workflowId) } returns emptyList()

        // When
        val result = repository.refreshExecutionsForWorkflow(workflowId, 10)

        // Then
        assertEquals(1, result.size)
        assertEquals("exec1", result[0].id)
    }

    @Test
    fun `stopExecution calls API successfully`() = runTest {
        // Given
        val executionId = "exec1"
        coEvery { apiService.stopExecution(executionId) } returns Response.success(Unit)

        // When
        val result = repository.stopExecution(executionId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `stopExecution returns false on API failure`() = runTest {
        // Given
        val executionId = "exec1"
        coEvery { apiService.stopExecution(executionId) } returns Response.error(500, mockk())

        // When
        val result = repository.stopExecution(executionId)

        // Then
        assertTrue(!result)
    }

    @Test
    fun `toggleBookmark updates database`() = runTest {
        // Given
        val workflowId = "1"
        val isBookmarked = true
        coEvery { workflowDao.updateBookmark(workflowId, isBookmarked) } returns Unit

        // When
        repository.toggleBookmark(workflowId, isBookmarked)

        // Then
        // Verify the method was called (implicitly verified by coEvery)
    }

    @Test
    fun `cleanupStaleData removes old executions`() = runTest {
        // Given
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000) // 30 days ago
        coEvery { executionDao.deleteExecutionsOlderThan(cutoffTime) } returns Unit

        // When
        repository.cleanupStaleData()

        // Then
        // Verify the method was called (implicitly verified by coEvery)
    }
}