package com.example.n8nmonitor.data.database

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    // Optimized queries with better performance

    @Query("SELECT * FROM workflows WHERE active = :active ORDER BY isBookmarked DESC, updatedAt DESC")
    fun getWorkflows(active: Boolean = true): Flow<List<WorkflowEntity>>

    // Paginated version for large datasets
    @Query("SELECT * FROM workflows WHERE active = :active ORDER BY isBookmarked DESC, updatedAt DESC")
    fun getWorkflowsPaged(active: Boolean = true): PagingSource<Int, WorkflowEntity>

    // Optimized query with LIMIT for performance
    @Query("SELECT * FROM workflows WHERE active = :active ORDER BY isBookmarked DESC, updatedAt DESC LIMIT :limit")
    suspend fun getWorkflowsLimited(active: Boolean = true, limit: Int = 50): List<WorkflowEntity>

    // Get workflows by status with pagination
    @Query("SELECT * FROM workflows WHERE active = :active AND lastExecutionStatus = :status ORDER BY updatedAt DESC")
    fun getWorkflowsByStatus(active: Boolean = true, status: String): PagingSource<Int, WorkflowEntity>

    // Get bookmarked workflows
    @Query("SELECT * FROM workflows WHERE isBookmarked = 1 ORDER BY updatedAt DESC")
    fun getBookmarkedWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :workflowId")
    suspend fun getWorkflow(workflowId: String): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkflows(workflows: List<WorkflowEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Query("UPDATE workflows SET lastExecutionStatus = :status, lastExecutionTime = :time WHERE id = :workflowId")
    suspend fun updateLastExecution(workflowId: String, status: String, time: String)

    @Query("UPDATE workflows SET isBookmarked = :bookmarked WHERE id = :workflowId")
    suspend fun updateBookmark(workflowId: String, bookmarked: Boolean)

    @Query("DELETE FROM workflows WHERE lastSyncTime < :cutoffTime")
    suspend fun deleteStaleWorkflows(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM workflows WHERE active = 1")
    suspend fun getActiveWorkflowCount(): Int

    // Additional optimized queries for caching and performance
    @Query("SELECT COUNT(*) FROM workflows WHERE active = :active AND lastExecutionStatus = :status")
    suspend fun getWorkflowCountByStatus(active: Boolean = true, status: String): Int

    @Query("SELECT COUNT(*) FROM workflows WHERE isBookmarked = 1")
    suspend fun getBookmarkedWorkflowCount(): Int

    // Batch operations for better performance
    @Update
    suspend fun updateWorkflows(workflows: List<WorkflowEntity>)

    // Get workflows that need sync (for caching strategy)
    @Query("SELECT * FROM workflows WHERE lastSyncTime < :syncThreshold ORDER BY lastSyncTime ASC LIMIT :limit")
    suspend fun getWorkflowsNeedingSync(syncThreshold: Long, limit: Int = 20): List<WorkflowEntity>

    // Get workflow IDs only for lightweight operations
    @Query("SELECT id FROM workflows WHERE active = :active")
    suspend fun getWorkflowIds(active: Boolean = true): List<String>

    // Search workflows by name (for future search functionality)
    @Query("SELECT * FROM workflows WHERE name LIKE '%' || :query || '%' AND active = :active ORDER BY isBookmarked DESC, updatedAt DESC")
    fun searchWorkflows(query: String, active: Boolean = true): Flow<List<WorkflowEntity>>
}