package com.example.n8nmonitor.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.example.n8nmonitor.data.repository.N8nRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URL
import java.net.MalformedURLException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

data class SettingsState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val pollIntervalMinutes: Int = 15,
    val isDarkMode: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isNotificationEnabled: Boolean = true,
    val isTestingConnection: Boolean = false,
    val testConnectionResult: String? = null,
    val baseUrlError: String? = null,
    val apiKeyError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val repository: N8nRepository
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
        val validationError = validateBaseUrl(url)
        _state.update { it.copy(baseUrl = url, baseUrlError = validationError) }
        
        if (validationError == null) {
            viewModelScope.launch {
                settingsDataStore.setBaseUrl(url)
            }
        }
    }

    fun updateApiKey(key: String) {
        val validationError = validateApiKey(key)
        _state.update { it.copy(apiKey = key, apiKeyError = validationError) }
        
        if (validationError == null) {
            viewModelScope.launch {
                settingsDataStore.setApiKey(key)
            }
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

    fun testConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isTestingConnection = true, testConnectionResult = null) }
            try {
                val result = repository.refreshWorkflows(active = true)
                if (result.isSuccess) {
                    _state.update { 
                        it.copy(
                            isTestingConnection = false, 
                            testConnectionResult = "Connection successful!"
                        ) 
                    }
                } else {
                    _state.update { 
                        it.copy(
                            isTestingConnection = false, 
                            testConnectionResult = "Connection failed: ${result.exceptionOrNull()?.message}"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isTestingConnection = false, 
                        testConnectionResult = "Connection failed: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun clearTestResult() {
        _state.update { it.copy(testConnectionResult = null) }
    }
    
    private fun validateBaseUrl(url: String): String? {
        return when {
            url.isBlank() -> "Base URL cannot be empty"
            !url.startsWith("http://") && !url.startsWith("https://") -> 
                "Base URL must start with http:// or https://"
            url.length > 2048 -> "URL is too long (maximum 2048 characters)"
            url.contains("<") || url.contains(">") || url.contains('"') || url.contains("'") ->
                "URL contains invalid characters"
            else -> {
                try {
                    val parsedUrl = URL(url)
                    val host = parsedUrl.host
                    
                    if (host.isNullOrBlank()) {
                        return "Invalid URL format: missing host"
                    }
                    
                    // Check for suspicious patterns
                    if (host.contains("localhost") || host == "127.0.0.1" || host == "::1") {
                        return "Localhost URLs are not allowed for security reasons"
                    }
                    
                    // Check for private IP ranges
                    if (isPrivateOrLocalIP(host)) {
                        return "Private IP addresses are not recommended for production use"
                    }
                    
                    // Check port range
                    val port = parsedUrl.port
                    if (port != -1 && (port < 1 || port > 65535)) {
                        return "Invalid port number: $port"
                    }
                    
                    // Additional security checks
                    if (host.startsWith(".") || host.endsWith(".") || host.contains("..")) {
                        return "Invalid hostname format"
                    }
                    
                    null // Valid URL
                } catch (e: MalformedURLException) {
                    "Invalid URL format: ${e.message}"
                }
            }
        }
    }
    
    private fun isPrivateOrLocalIP(host: String): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isLoopbackAddress || address.isLinkLocalAddress || 
            address.isSiteLocalAddress || address.isAnyLocalAddress
        } catch (e: UnknownHostException) {
            // If we can't resolve the host, it's not a private IP
            false
        }
    }
    
    private fun validateApiKey(key: String): String? {
        return when {
            key.isBlank() -> "API key cannot be empty"
            key.length < 10 -> "API key seems too short (minimum 10 characters)"
            key.length > 512 -> "API key is too long (maximum 512 characters)"
            key.contains(" ") -> "API key should not contain spaces"
            key.contains("\n") || key.contains("\r") || key.contains("\t") ->
                "API key should not contain line breaks or tabs"
            key.contains("<") || key.contains(">") || key.contains('"') || key.contains("'") ->
                "API key contains invalid characters"
            key.startsWith(" ") || key.endsWith(" ") ->
                "API key should not have leading or trailing spaces"
            key.all { it.isDigit() } -> "API key should not be all numbers"
            key.all { it.isLetter() && it.isLowerCase() } -> 
                "API key should contain mixed case or special characters"
            key == "password" || key == "123456" || key == "admin" || key == "test" ->
                "API key appears to be a common weak value"
            else -> null // Valid API key
        }
    }
    
    fun isConfigurationValid(): Boolean {
        val currentState = _state.value
        return currentState.baseUrlError == null && 
               currentState.apiKeyError == null &&
               currentState.baseUrl.isNotBlank() &&
               currentState.apiKey.isNotBlank()
    }
}