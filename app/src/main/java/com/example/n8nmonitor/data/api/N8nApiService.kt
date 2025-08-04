package com.example.n8nmonitor.data.api

import com.example.n8nmonitor.data.dto.ExecutionDto
import com.example.n8nmonitor.data.dto.ExecutionsResponseDto
import com.example.n8nmonitor.data.dto.WorkflowDto
import retrofit2.http.*

interface N8nApiService {

    @GET("api/v1/workflows")
    suspend fun getWorkflows(
        @Query("active") active: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("tags[]") tags: List<String>? = null
    ): List<WorkflowDto>

    @GET("api/v1/workflows/{workflowId}")
    suspend fun getWorkflow(
        @Path("workflowId") workflowId: String
    ): WorkflowDto

    @GET("api/v1/executions")
    suspend fun getExecutions(
        @Query("workflowId") workflowId: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null
    ): ExecutionsResponseDto

    @GET("api/v1/executions/{executionId}")
    suspend fun getExecution(
        @Path("executionId") executionId: String,
        @Query("includeData") includeData: Boolean = true
    ): ExecutionDto

    @POST("api/v1/executions/{executionId}/stop")
    suspend fun stopExecution(
        @Path("executionId") executionId: String
    ): retrofit2.Response<Unit>
} 