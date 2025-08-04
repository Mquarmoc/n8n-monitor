package com.example.n8nmonitor.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workflows",
    indices = [
        Index(value = ["active"]),
        Index(value = ["updatedAt"])
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