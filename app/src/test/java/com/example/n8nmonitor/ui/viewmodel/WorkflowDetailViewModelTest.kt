package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.ui.state.WorkflowDetailState
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
        
        coEvery { repository.getWorkflowDetails(workflowId) } returns Pair(workflow, executions)

        // When
        viewModel.loadWorkflowDetails()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(workflow, viewModel.state.value.workflow)
        assertEquals(1, viewModel.state.value.executions.size)
        assertEquals("exec1", viewModel.state.value.executions[0].id)
    }

    @Test
    fun `loadWorkflowDetails sets error state on exception`() = runTest {
        // Given
        val workflowId = "1"
        coEvery { repository.getWorkflowDetails(workflowId) } throws RuntimeException("Network error")

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
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                dataChunkPath = null
            )
        )
        coEvery { repository.refreshExecutions(workflowId) } returns executions

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
        coEvery { repository.refreshExecutions(workflowId) } throws RuntimeException("API error")

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
    fun `stopExecution calls repository and updates state`() = runTest {
        // Given
        val executionId = "exec1"
        coEvery { repository.stopExecution(executionId) } returns true

        // When
        val result = viewModel.stopExecution(executionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(result)
    }

    @Test
    fun `stopExecution returns false on repository failure`() = runTest {
        // Given
        val executionId = "exec1"
        coEvery { repository.stopExecution(executionId) } returns false

        // When
        val result = viewModel.stopExecution(executionId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(result)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given
        viewModel.state.value = WorkflowDetailState(
            workflow = null,
            executions = emptyList(),
            isLoading = false,
            isRefreshing = false,
            error = "Some error"
        )

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `setExecutionToStop sets executionToStop state`() = runTest {
        // Given
        val executionId = "exec1"

        // When
        viewModel.setExecutionToStop(executionId)

        // Then
        assertEquals(executionId, viewModel.state.value.executionToStop)
    }

    @Test
    fun `clearExecutionToStop clears executionToStop state`() = runTest {
        // Given
        viewModel.state.value = WorkflowDetailState(
            workflow = null,
            executions = emptyList(),
            isLoading = false,
            isRefreshing = false,
            error = null,
            executionToStop = "exec1"
        )

        // When
        viewModel.clearExecutionToStop()

        // Then
        assertNull(viewModel.state.value.executionToStop)
    }
} 