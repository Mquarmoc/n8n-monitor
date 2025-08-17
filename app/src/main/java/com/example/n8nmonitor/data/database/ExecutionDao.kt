package com.example.n8nmonitor.data.database

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {

    // Optimized queries with better performance and pagination

    @Query("SELECT * FROM executions WHERE workflowId = :workflowId ORDER BY startTime DESC LIMIT :limit")
    fun getExecutionsForWorkflow(workflowId: String, limit: Int = 10): Flow<List<ExecutionEntity>>

    // Paginated version for large datasets
    @Query("SELECT * FROM executions WHERE workflowId = :workflowId ORDER BY startTime DESC")
    fun getExecutionsForWorkflowPaged(workflowId: String): PagingSource<Int, ExecutionEntity>

    // Get executions by status with pagination
    @Query("SELECT * FROM executions WHERE status = :status ORDER BY startTime DESC")
    fun getExecutionsByStatus(status: String): PagingSource<Int, ExecutionEntity>

    // Get recent executions across all workflows
    @Query("SELECT * FROM executions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentExecutions(limit: Int = 20): List<ExecutionEntity>

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

    // Additional optimized queries for analytics and caching
    @Query("SELECT COUNT(*) FROM executions WHERE workflowId = :workflowId")
    suspend fun getExecutionCountForWorkflow(workflowId: String): Int

    @Query("SELECT COUNT(*) FROM executions WHERE status = :status")
    suspend fun getExecutionCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM executions WHERE startTime > :since")
    suspend fun getExecutionCountSince(since: String): Int

    // Batch operations for better performance
    @Update
    suspend fun updateExecutions(executions: List<ExecutionEntity>)

    // Get executions that need sync (for caching strategy)
    @Query("SELECT * FROM executions WHERE lastSyncTime < :syncThreshold ORDER BY lastSyncTime ASC LIMIT :limit")
    suspend fun getExecutionsNeedingSync(syncThreshold: Long, limit: Int = 50): List<ExecutionEntity>

    // Get execution statistics for dashboard
    @Query("SELECT status, COUNT(*) as count FROM executions WHERE workflowId = :workflowId AND startTime > :since GROUP BY status")
    suspend fun getExecutionStatsByWorkflow(workflowId: String, since: String): List<ExecutionStats>

    @Query("SELECT status, COUNT(*) as count FROM executions WHERE startTime > :since GROUP BY status")
    suspend fun getOverallExecutionStats(since: String): List<ExecutionStats>

    // Get latest execution for each workflow (for dashboard summary)
    @Query("SELECT * FROM executions e1 WHERE e1.startTime = (SELECT MAX(e2.startTime) FROM executions e2 WHERE e2.workflowId = e1.workflowId) ORDER BY e1.startTime DESC")
    suspend fun getLatestExecutionPerWorkflow(): List<ExecutionEntity>

    // Cleanup old executions beyond a certain count per workflow
    @Query("DELETE FROM executions WHERE id IN (SELECT id FROM executions WHERE workflowId = :workflowId ORDER BY startTime DESC LIMIT -1 OFFSET :keepCount)")
    suspend fun cleanupOldExecutions(workflowId: String, keepCount: Int = 100)

    // Get execution IDs only for lightweight operations
    @Query("SELECT id FROM executions WHERE workflowId = :workflowId ORDER BY startTime DESC")
    suspend fun getExecutionIds(workflowId: String): List<String>
}