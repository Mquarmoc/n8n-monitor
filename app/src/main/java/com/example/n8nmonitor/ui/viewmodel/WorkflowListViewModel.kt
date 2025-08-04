package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.ui.state.WorkflowListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkflowListViewModel @Inject constructor(
    private val repository: N8nRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkflowListState())
    val state: StateFlow<WorkflowListState> = _state.asStateFlow()

    init {
        loadWorkflows()
    }

    fun loadWorkflows() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                repository.refreshWorkflows(active = true)
                    .onSuccess { workflows ->
                        _state.update { 
                            it.copy(
                                workflows = workflows,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load workflows"
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

    fun refreshWorkflows() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            
            try {
                repository.refreshWorkflows(active = true)
                    .onSuccess { workflows ->
                        _state.update { 
                            it.copy(
                                workflows = workflows,
                                isRefreshing = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { exception ->
                        _state.update { 
                            it.copy(
                                isRefreshing = false,
                                error = exception.message ?: "Failed to refresh workflows"
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

    fun toggleBookmark(workflowId: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.updateBookmark(workflowId, isBookmarked)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
} 