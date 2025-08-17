package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n8nmonitor.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StartupState(
    val isLoading: Boolean = true,
    val isConfigured: Boolean = false,
    val startDestination: String = "workflows"
)

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkInitialConfiguration()
    }

    private fun checkInitialConfiguration() {
        viewModelScope.launch {
            try {
                // Combine both settings flows to check if both are configured
                val baseUrl = settingsDataStore.baseUrl.first()
                val apiKey = settingsDataStore.apiKey.first()
                
                val isConfigured = !baseUrl.isNullOrBlank() && !apiKey.isNullOrBlank()
                val startDestination = if (isConfigured) "workflows" else "settings"
                
                _state.value = StartupState(
                    isLoading = false,
                    isConfigured = isConfigured,
                    startDestination = startDestination
                )
            } catch (e: Exception) {
                // If there's an error reading settings, default to settings screen
                _state.value = StartupState(
                    isLoading = false,
                    isConfigured = false,
                    startDestination = "settings"
                )
            }
        }
    }

    fun retryConfiguration() {
        checkInitialConfiguration()
    }
}