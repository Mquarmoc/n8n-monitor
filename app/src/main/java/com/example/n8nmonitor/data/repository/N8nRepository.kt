package com.example.n8nmonitor.data.repository

import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.database.ExecutionDao
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.database.WorkflowDao
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.data.dto.ExecutionDto
import com.example.n8nmonitor.data.dto.WorkflowDto
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.example.n8nmonitor.data.exceptions.ApiConfigurationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.HttpException
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class N8nRepository @Inject constructor(
    private val apiService: N8nApiService,
    protected val workflowDao: WorkflowDao,
    protected val executionDao: ExecutionDao,
    private val settingsDataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    
    private suspend fun createDynamicApiService(): N8nApiService {
        val baseUrl = settingsDataStore.baseUrl.first()
        val apiKey = settingsDataStore.apiKey.first()
        
        // Validate configuration
        when {
            baseUrl.isNullOrBlank() && apiKey.isNullOrBlank() -> {
                throw ApiConfigurationException.missingConfiguration()
            }
            baseUrl.isNullOrBlank() -> {
                throw ApiConfigurationException.missingBaseUrl()
            }
            apiKey.isNullOrBlank() -> {
                throw ApiConfigurationException.missingApiKey()
            }
        }
        
        // Validate URL format
        val normalizedUrl = try {
            val url = if (!baseUrl!!.endsWith("/")) "$baseUrl/" else baseUrl
            URL(url) // This will throw MalformedURLException if invalid
            url
        } catch (e: MalformedURLException) {
            throw ApiConfigurationException.invalidBaseUrl(baseUrl!!)
        }
        
        val dynamicClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val authenticatedRequest = originalRequest.newBuilder()
                    .header("X-N8N-API-KEY", apiKey!!)
                    .header("Accept", "application/json")
                    .header("User-Agent", "n8n-monitor-android/1.0.0")
                    .build()
                chain.proceed(authenticatedRequest)
            }
            .build()
        
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(dynamicClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApiService::class.java)
    }

    // Workflow operations
    fun getWorkflows(active: Boolean = true): Flow<List<WorkflowEntity>> {
        return workflowDao.getWorkflows(active)
    }

    open suspend fun refreshWorkflows(active: Boolean = true): Result<List<WorkflowEntity>> {
        return try {
            val dynamicApiService = createDynamicApiService()
            val workflows = dynamicApiService.getWorkflows(active = active)
            val entities = workflows.map { workflow -> workflow.toEntity() }
            workflowDao.insertWorkflows(entities)
            Result.success(entities)
        } catch (e: ApiConfigurationException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please check your API key in Settings."
                403 -> "Access denied. Please verify your API key permissions."
                404 -> "n8n server not found. Please check your server URL in Settings."
                500 -> "n8n server error. Please try again later."
                else -> "Network error (${e.code()}). Please check your connection and try again."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Connection failed. Please check your internet connection and server URL."))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown error occurred"}"))
        }
    }

    open suspend fun getWorkflow(workflowId: String): Result<WorkflowEntity> {
        return try {
            val dynamicApiService = createDynamicApiService()
            val workflow = dynamicApiService.getWorkflow(workflowId)
            val entity = workflow.toEntity()
            workflowDao.insertWorkflow(entity)
            Result.success(entity)
        } catch (e: ApiConfigurationException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please check your API key in Settings."
                403 -> "Access denied. Please verify your API key permissions."
                404 -> "Workflow not found. It may have been deleted."
                500 -> "n8n server error. Please try again later."
                else -> "Network error (${e.code()}). Please check your connection and try again."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Connection failed. Please check your internet connection and server URL."))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown error occurred"}"))
        }
    }

    suspend fun updateBookmark(workflowId: String, bookmarked: Boolean) {
        workflowDao.updateBookmark(workflowId, bookmarked)
    }

    // Execution operations
    fun getExecutionsForWorkflow(workflowId: String, limit: Int = 10): Flow<List<ExecutionEntity>> {
        return executionDao.getExecutionsForWorkflow(workflowId, limit)
    }

    open suspend fun refreshExecutionsForWorkflow(workflowId: String, limit: Int = 20): Result<List<ExecutionEntity>> {
        return try {
            val dynamicApiService = createDynamicApiService()
            val executions = dynamicApiService.getExecutions(workflowId = workflowId, limit = limit)
            val entities = executions.results.map { execution -> execution.toEntity() }
            executionDao.insertExecutions(entities)
            Result.success(entities)
        } catch (e: ApiConfigurationException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please check your API key in Settings."
                403 -> "Access denied. Please verify your API key permissions."
                404 -> "Workflow not found or no executions available."
                500 -> "n8n server error. Please try again later."
                else -> "Network error (${e.code()}). Please check your connection and try again."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Connection failed. Please check your internet connection and server URL."))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown error occurred"}"))
        }
    }

    suspend fun getExecution(executionId: String): Result<ExecutionEntity> {
        return try {
            val dynamicApiService = createDynamicApiService()
            val execution = dynamicApiService.getExecution(executionId)
            val entity = execution.toEntity()
            executionDao.insertExecution(entity)
            Result.success(entity)
        } catch (e: ApiConfigurationException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please check your API key in Settings."
                403 -> "Access denied. Please verify your API key permissions."
                404 -> "Execution not found. It may have been deleted."
                500 -> "n8n server error. Please try again later."
                else -> "Network error (${e.code()}). Please check your connection and try again."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Connection failed. Please check your internet connection and server URL."))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown error occurred"}"))
        }
    }

    open suspend fun stopExecution(executionId: String): Result<Unit> {
        return try {
            val dynamicApiService = createDynamicApiService()
            dynamicApiService.stopExecution(executionId)
            Result.success(Unit)
        } catch (e: ApiConfigurationException) {
            Result.failure(e)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please check your API key in Settings."
                403 -> "Access denied. Please verify your API key permissions."
                404 -> "Execution not found or already completed."
                409 -> "Execution cannot be stopped in its current state."
                500 -> "n8n server error. Please try again later."
                else -> "Network error (${e.code()}). Please check your connection and try again."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("Connection failed. Please check your internet connection and server URL."))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message ?: "Unknown error occurred"}"))
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

    protected fun WorkflowDto.toEntity(): WorkflowEntity {
        return WorkflowEntity(
            id = id,
            name = name,
            active = active,
            updatedAt = updatedAt,
            tags = tags?.joinToString(",")
        )
    }

    protected fun ExecutionDto.toEntity(): ExecutionEntity {
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