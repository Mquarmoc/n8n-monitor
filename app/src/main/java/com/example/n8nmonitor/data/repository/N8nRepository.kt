package com.example.n8nmonitor.data.repository

import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.database.WorkflowDao
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.dto.ExecutionDto
import com.example.n8nmonitor.data.dto.WorkflowDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class N8nRepository @Inject constructor(
    private val apiService: N8nApiService,
    private val workflowDao: WorkflowDao,
    private val executionDao: ExecutionDao
) {

    // Workflow operations
    fun getWorkflows(active: Boolean = true): Flow<List<WorkflowEntity>> {
        return workflowDao.getWorkflows(active)
    }

    suspend fun refreshWorkflows(active: Boolean = true): Result<List<WorkflowEntity>> {
        return try {
            val workflows = apiService.getWorkflows(active = active)
            val entities = workflows.map { it.toEntity() }
            workflowDao.insertWorkflows(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflow(workflowId: String): Result<WorkflowDto> {
        return try {
            val workflow = apiService.getWorkflow(workflowId)
            Result.success(workflow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBookmark(workflowId: String, bookmarked: Boolean) {
        workflowDao.updateBookmark(workflowId, bookmarked)
    }

    // Execution operations
    fun getExecutionsForWorkflow(workflowId: String, limit: Int = 10): Flow<List<ExecutionEntity>> {
        return executionDao.getExecutionsForWorkflow(workflowId, limit)
    }

    suspend fun refreshExecutionsForWorkflow(workflowId: String, limit: Int = 10): Result<List<ExecutionEntity>> {
        return try {
            val response = apiService.getExecutions(
                workflowId = workflowId,
                status = "success,failed",
                limit = limit
            )
            val entities = response.results.map { it.toEntity() }
            executionDao.insertExecutions(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExecution(executionId: String): Result<ExecutionDto> {
        return try {
            val execution = apiService.getExecution(executionId)
            Result.success(execution)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopExecution(executionId: String): Result<Boolean> {
        return try {
            val response = apiService.stopExecution(executionId)
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Background monitoring
    suspend fun getFailedExecutionsSince(since: String): List<ExecutionEntity> {
        return executionDao.getFailedExecutionsSince(since)
    }

    // Cleanup
    suspend fun cleanupStaleData() {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        workflowDao.deleteStaleWorkflows(cutoffTime)
        executionDao.deleteStaleExecutions(cutoffTime)
    }

    private fun WorkflowDto.toEntity(): WorkflowEntity {
        return WorkflowEntity(
            id = id,
            name = name,
            active = active,
            updatedAt = updatedAt,
            tags = tags?.joinToString(",")
        )
    }

    private fun ExecutionDto.toEntity(): ExecutionEntity {
        return ExecutionEntity(
            id = id,
            workflowId = workflowId,
            status = status,
            startTime = start,
            endTime = end,
            duration = timing?.duration
        )
    }
} 