package com.example.n8nmonitor.data.repository

import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.database.WorkflowDao
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

/**
 * Test-specific repository that bypasses the async flow operations in createDynamicApiService
 * to avoid UncompletedCoroutinesError in unit tests.
 */
class TestN8nRepository(
    private val apiService: N8nApiService,
    workflowDao: WorkflowDao,
    executionDao: ExecutionDao,
    settingsDataStore: SettingsDataStore,
    okHttpClient: OkHttpClient,
    moshi: Moshi
) : N8nRepository(apiService, workflowDao, executionDao, settingsDataStore, okHttpClient, moshi) {
    
    // Override methods that use createDynamicApiService to use the injected apiService directly
    override suspend fun refreshWorkflows(active: Boolean): Result<List<WorkflowEntity>> {
        return try {
            val workflows = apiService.getWorkflows(active = active)
            val entities = workflows.map { workflow -> workflow.toEntity() }
            workflowDao.insertWorkflows(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getWorkflow(workflowId: String): Result<WorkflowEntity> {
        return try {
            val workflow = apiService.getWorkflow(workflowId)
            val entity = workflow.toEntity()
            workflowDao.insertWorkflow(entity)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshExecutionsForWorkflow(workflowId: String, limit: Int): Result<List<ExecutionEntity>> {
        return try {
            val executions = apiService.getExecutions(workflowId = workflowId, limit = limit)
            val entities = executions.results.map { execution -> execution.toEntity() }
            executionDao.insertExecutions(entities)
            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun stopExecution(executionId: String): Result<Unit> {
        return try {
            apiService.stopExecution(executionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}