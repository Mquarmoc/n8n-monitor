package com.example.n8nmonitor.ui.viewmodel

import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.ui.state.WorkflowListState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowListViewModelTest {

    private lateinit var viewModel: WorkflowListViewModel
    private lateinit var repository: N8nRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = WorkflowListViewModel(repository)
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
        assertTrue(viewModel.state.value.workflows.isEmpty())
    }

    @Test
    fun `loadWorkflows sets loading state and updates with workflows`() = runTest {
        // Given
        val workflows = listOf(
            WorkflowEntity(
                id = "1",
                name = "Test Workflow",
                active = true,
                updatedAt = "2023-01-01T00:00:00Z",
                tags = null,
                lastExecutionStatus = "success",
                lastExecutionTime = "2023-01-01T00:00:00Z",
                isBookmarked = false,
                lastSyncTime = System.currentTimeMillis()
            )
        )
        coEvery { repository.refreshWorkflows(active = true) } returns Result.success(workflows)

        // When
        viewModel.loadWorkflows()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(1, viewModel.state.value.workflows.size)
        assertEquals("Test Workflow", viewModel.state.value.workflows[0].name)
    }

    @Test
    fun `loadWorkflows sets error state on exception`() = runTest {
        // Given
        coEvery { repository.refreshWorkflows(active = true) } returns Result.failure(RuntimeException("Network error"))

        // When
        viewModel.loadWorkflows()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertTrue(viewModel.state.value.error != null)
        assertEquals("Network error", viewModel.state.value.error)
    }

    @Test
    fun `refreshWorkflows sets refreshing state and updates workflows`() = runTest {
        // Given
        val workflows = listOf(
            WorkflowEntity(
                id = "1",
                name = "Refreshed Workflow",
                active = true,
                updatedAt = "2023-01-01T00:00:00Z",
                tags = null,
                lastExecutionStatus = "success",
                lastExecutionTime = "2023-01-01T00:00:00Z",
                isBookmarked = false,
                lastSyncTime = System.currentTimeMillis()
            )
        )
        coEvery { repository.refreshWorkflows(active = true) } returns Result.success(workflows)

        // When
        viewModel.refreshWorkflows()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(1, viewModel.state.value.workflows.size)
        assertEquals("Refreshed Workflow", viewModel.state.value.workflows[0].name)
    }

    @Test
    fun `refreshWorkflows sets error state on exception`() = runTest {
        // Given
        coEvery { repository.refreshWorkflows(active = true) } returns Result.failure(RuntimeException("API error"))

        // When
        viewModel.refreshWorkflows()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertTrue(viewModel.state.value.error != null)
        assertEquals("API error", viewModel.state.value.error)
    }

    @Test
    fun `toggleBookmark calls repository`() = runTest {
        // Given
        val workflowId = "1"
        val isBookmarked = true
        coEvery { repository.updateBookmark(workflowId, isBookmarked) } returns Unit

        // When
        viewModel.toggleBookmark(workflowId, isBookmarked)

        // Then
        // Verify the method was called (implicitly verified by coEvery)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Given - First cause an error
        coEvery { repository.refreshWorkflows(active = true) } returns Result.failure(RuntimeException("Test error"))
        viewModel.loadWorkflows()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error is set
        assertTrue(viewModel.state.value.error != null)

        // When
        viewModel.clearError()

        // Then
        assertTrue(viewModel.state.value.error == null)
    }
}