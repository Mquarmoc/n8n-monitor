package com.example.n8nmonitor.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {

    @Query("SELECT * FROM executions WHERE workflowId = :workflowId ORDER BY startTime DESC LIMIT :limit")
    fun getExecutionsForWorkflow(workflowId: String, limit: Int = 10): Flow<List<ExecutionEntity>>

    @Query("SELECT * FROM executions WHERE id = :executionId")
    suspend fun getExecution(executionId: String): ExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExecutions(executions: List<ExecutionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExecution(execution: ExecutionEntity)

    @Query("SELECT * FROM executions WHERE status = 'failed' AND startTime > :since ORDER BY startTime DESC")
    suspend fun getFailedExecutionsSince(since: String): List<ExecutionEntity>

    @Query("DELETE FROM executions WHERE lastSyncTime < :cutoffTime")
    suspend fun deleteStaleExecutions(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM executions WHERE workflowId = :workflowId AND status = 'failed' AND startTime > :since")
    suspend fun getFailedExecutionCount(workflowId: String, since: String): Int
}