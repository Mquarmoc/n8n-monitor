package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n8nmonitor.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val pollIntervalMinutes: Int = 15,
    val isDarkMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    // Expose settings as flows for the UI
    val baseUrl = settingsDataStore.baseUrl
    val apiKey = settingsDataStore.apiKey
    val pollIntervalMinutes = settingsDataStore.pollIntervalMinutes
    val isDarkMode = settingsDataStore.isDarkMode
    val isBiometricEnabled = settingsDataStore.isBiometricEnabled
    val isNotificationEnabled = settingsDataStore.isNotificationEnabled

    fun updateBaseUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setBaseUrl(url)
            _state.update { it.copy(baseUrl = url) }
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setApiKey(key)
            _state.update { it.copy(apiKey = key) }
        }
    }

    fun updatePollInterval(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setPollIntervalMinutes(minutes)
            _state.update { it.copy(pollIntervalMinutes = minutes) }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(enabled)
            _state.update { it.copy(isDarkMode = enabled) }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setBiometricEnabled(enabled)
            _state.update { it.copy(isBiometricEnabled = enabled) }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationEnabled(enabled)
            _state.update { it.copy(isNotificationEnabled = enabled) }
        }
    }

    fun clearSettings() {
        viewModelScope.launch {
            settingsDataStore.clearSettings()
            _state.update { SettingsState() }
        }
    }
} 