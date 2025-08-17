package com.example.n8nmonitor.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workflows",
    indices = [
        Index(value = ["active"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isBookmarked"]),
        Index(value = ["lastExecutionStatus"]),
        Index(value = ["lastSyncTime"]),
        // Composite indexes for common query patterns
        Index(value = ["active", "isBookmarked", "updatedAt"]),
        Index(value = ["active", "lastExecutionStatus"]),
        Index(value = ["lastSyncTime", "active"])
    ]
)
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean,
    val updatedAt: String,
    val tags: String?, // JSON array as string
    val lastExecutionStatus: String? = null,
    val lastExecutionTime: String? = null,
    val isBookmarked: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis()
)