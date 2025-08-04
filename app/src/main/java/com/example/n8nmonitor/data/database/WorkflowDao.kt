package com.example.n8nmonitor.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {

    @Query("SELECT * FROM workflows WHERE active = :active ORDER BY isBookmarked DESC, updatedAt DESC")
    fun getWorkflows(active: Boolean = true): Flow<List<WorkflowEntity>>

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
}