package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.ui.state.WorkflowDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkflowDetailViewModel @Inject constructor(
    private val repository: N8nRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workflowId: String = checkNotNull(savedStateHandle["workflowId"])

    private val _state = MutableStateFlow(WorkflowDetailState())
    val state: StateFlow<WorkflowDetailState> = _state.asStateFlow()

    init {
        loadWorkflowDetails()
    }

    fun loadWorkflowDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load workflow info
                repository.getWorkflow(workflowId)
                    .onSuccess { workflowDto ->
                        // Convert to entity for consistency
                        val workflowEntity = com.example.n8nmonitor.data.database.WorkflowEntity(
                            id = workflowDto.id,
                            name = workflowDto.name,
                            active = workflowDto.active,
                            updatedAt = workflowDto.updatedAt,
                            tags = workflowDto.tags?.joinToString(",")
                        )
                        
                        _state.update { it.copy(workflow = workflowEntity) }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load workflow"
                            )
                        }
                        return@launch
                    }

                // Load executions
                repository.refreshExecutionsForWorkflow(workflowId, limit = 10)
                    .onSuccess { executions ->
                        _state.update { 
                            it.copy(
                                executions = executions,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load executions"
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun refreshExecutions() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            
            try {
                repository.refreshExecutionsForWorkflow(workflowId, limit = 10)
                    .onSuccess { executions ->
                        _state.update { 
                            it.copy(
                                executions = executions,
                                isRefreshing = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(
                                isRefreshing = false,
                                error = exception.message ?: "Failed to refresh executions"
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun stopExecution(executionId: String) {
        viewModelScope.launch {
            try {
                repository.stopExecution(executionId)
                    .onSuccess { success ->
                        if (success) {
                            // Refresh executions to show updated status
                            refreshExecutions()
                        } else {
                            _state.update { 
                                it.copy(error = "Failed to stop execution")
                            }
                        }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(error = exception.message ?: "Failed to stop execution")
                        }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
} 