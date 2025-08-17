package com.example.n8nmonitor.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "executions",
    foreignKeys = [
        ForeignKey(
            entity = WorkflowEntity::class,
            parentColumns = ["id"],
            childColumns = ["workflowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workflowId"]),
        Index(value = ["status"]),
        Index(value = ["startTime"]),
        Index(value = ["lastSyncTime"]),
        // Composite indexes for common query patterns
        Index(value = ["workflowId", "startTime"]),
        Index(value = ["workflowId", "status"]),
        Index(value = ["status", "startTime"]),
        Index(value = ["workflowId", "status", "startTime"]),
        Index(value = ["lastSyncTime", "workflowId"])
    ]
)
data class ExecutionEntity(
    @PrimaryKey val id: String,
    val workflowId: String,
    val status: String,
    val startTime: String?,
    val endTime: String?,
    val duration: Long?,
    val dataChunkPath: String? = null, // Path to external file for large logs
    val lastSyncTime: Long = System.currentTimeMillis()
)