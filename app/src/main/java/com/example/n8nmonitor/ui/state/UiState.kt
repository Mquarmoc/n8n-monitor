package com.example.n8nmonitor.ui.state

import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.database.WorkflowEntity

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

data class WorkflowListState(
    val workflows: List<WorkflowEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

data class WorkflowDetailState(
    val workflow: WorkflowEntity? = null,
    val executions: List<ExecutionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false
)

data class SettingsState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val pollIntervalMinutes: Int = 15,
    val isDarkMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
) 