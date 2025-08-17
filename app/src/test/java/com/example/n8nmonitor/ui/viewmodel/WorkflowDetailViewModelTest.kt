package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.database.ExecutionEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowDetailViewModelTest {

    private lateinit var viewModel: WorkflowDetailViewModel
    private lateinit var repository: N8nRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("workflowId" to "1"))
        viewModel = WorkflowDetailViewModel(repository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() = runTest {
        // Then
        assertTrue(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertNull(viewModel.state.value.workflow)
        assertTrue(viewModel.state.value.executions.isEmpty())
    }

    @Test
    fun `loadWorkflowDetails sets loading state and updates with workflow and executions`() = runTest {
        // Given
        val workflowId = "1"
        val expectedWorkflow = WorkflowEntity(
            id = workflowId,
            name = "Test Workflow",
            active = true,
            updatedAt = "2023-01-01T00:00:00Z",
            tags = null
        )
        val executions = listOf(
            ExecutionEntity(
                id = "exec1",
                workflowId = workflowId,
                status = "success",
                startTime = "2023-01-01T00:00:00Z",
                endTime = "2023-01-01T00:01:00Z",
                duration = 60000L
            )
        )
        
        coEvery { repository.getWorkflow(workflowId) } returns Result.success(expectedWorkflow)
        coEvery { repository.refreshExecutionsForWorkflow(workflowId, 10) } returns Result.success(executions)

        // When
        viewModel.loadWorkflowDetails()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(expectedWorkflow, viewModel.state.value.workflow)
        assertEquals(1, viewModel.state.value.executions.size)
        assertEquals("exec1", viewModel.state.value.executions[0].id)
    }

    @Test
    fun `loadWorkflowDetails sets error state on exception`() = runTest {
        // Given
        val workflowId = "1"
        coEvery { repository.getWorkflow(workflowId) } returns Result.failure(RuntimeException("Network error"))

        // When
        viewModel.loadWorkflowDetails()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertTrue(viewModel.state.value.error != null)
        assertEquals("Network error", viewModel.state.value.error)
    }

    @Test
    fun `refreshExecutions sets refreshing state and updates executions`() = runTest {
        // Given
        val workflowId = "1"
        val executions = listOf(
            ExecutionEntity(
                id = "exec2",
                workflowId = workflowId,
                status = "success",
                startTime = "2023-01-01T00:00:00Z",
                endTime = "2023-01-01T00:01:00Z",
                duration = 60000L
            )
        )
        coEvery { repository.refreshExecutionsForWorkflow(workflowId, 10) } returns Result.success(executions)

        // When
        viewModel.refreshExecutions()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(1, viewModel.state.value.executions.size)
        assertEquals("exec2", viewModel.state.value.executions[0].id)
    }

    @Test
    fun `refreshExecutions sets error state on exception`() = runTest {
        // Given
        val workflowId = "1"
        coEvery { repository.refreshExecutionsForWorkflow(workflowId, 10) } returns Result.failure(RuntimeException("API error"))

        // When
        viewModel.refreshExecutions()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertTrue(viewModel.state.value.error != null)
        assertEquals("API error", viewModel.state.value.error)
    }

    @Test
    fun `stopExecution calls repository and refreshes on success`() = runTest {
        // Given
        val executionId = "exec1"
        val updatedExecutions = listOf(
            ExecutionEntity(
                id = "exec1",
                workflowId = "1",
                status = "stopped",
                startTime = "2023-01-01T00:00:00Z",
                endTime = "2023-01-01T00:01:00Z",
                duration = 60000L
            )
        )
        coEvery { repository.stopExecution(executionId) } returns Result.success(Unit)
        coEvery { repository.refreshExecutionsForWorkflow("1", 10) } returns Result.success(updatedExecutions)

        // When
        viewModel.stopExecution(executionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.state.value.executions.size)
        assertEquals("stopped", viewModel.state.value.executions[0].status)
    }

    @Test
    fun `stopExecution sets error on repository failure`() = runTest {
        // Given
        val executionId = "exec1"
        coEvery { repository.stopExecution(executionId) } returns Result.failure(RuntimeException("Stop failed"))

        // When
        viewModel.stopExecution(executionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals("Stop failed", viewModel.state.value.error)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given - First cause an error
        coEvery { repository.getWorkflow("1") } returns Result.failure(RuntimeException("Test error"))
        viewModel.loadWorkflowDetails()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error is set
        assertTrue(viewModel.state.value.error != null)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.state.value.error)
    }
}