package com.n8nmonitor.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: MonitorSettings = MonitorSettings(),
    val workflows: List<Workflow> = emptyList(),
    val executions: List<Execution> = emptyList(),
    val loading: Boolean = false,
    val testing: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val api = N8nApi()
    private val _ui = MutableStateFlow(
        runCatching { MainUiState(settings = store.load()) }
            .getOrElse { MainUiState(error = "Encrypted settings could not be opened.") },
    )
    val ui = _ui.asStateFlow()

    fun saveSettings(
        baseUrl: String,
        apiKey: String,
        pollMinutes: Int,
        notificationsEnabled: Boolean,
    ): Boolean {
        validateMonitorSettings(baseUrl, apiKey)?.let {
            _ui.update { state -> state.copy(error = it, notice = null) }
            return false
        }
        val settings = MonitorSettings(
            baseUrl = baseUrl.trim().trimEnd('/'),
            apiKey = apiKey.trim(),
            pollMinutes = pollMinutes.coerceAtLeast(15),
            notificationsEnabled = notificationsEnabled,
        )
        return runCatching {
            store.save(settings)
            FailureMonitorWorker.schedule(getApplication(), settings)
        }.fold(
            onSuccess = {
                _ui.update { state ->
                    state.copy(settings = settings, notice = "Settings saved.", error = null)
                }
                true
            },
            onFailure = {
                _ui.update { state ->
                    state.copy(error = "Settings could not be saved.", notice = null)
                }
                false
            },
        )
    }

    fun testConnection(baseUrl: String, apiKey: String) {
        validateMonitorSettings(baseUrl, apiKey)?.let {
            _ui.update { state -> state.copy(error = it, notice = null) }
            return
        }
        val settings = MonitorSettings(baseUrl.trim().trimEnd('/'), apiKey.trim())
        viewModelScope.launch {
            _ui.update { it.copy(testing = true, error = null, notice = null) }
            runCatching { api.workflows(settings, limit = 1) }.fold(
                onSuccess = {
                    _ui.update { it.copy(testing = false, notice = "Connection successful.") }
                },
                onFailure = { failure ->
                    _ui.update {
                        it.copy(testing = false, error = failure.message ?: "Connection failed.")
                    }
                },
            )
        }
    }

    fun loadWorkflows() = load {
        val settings = _ui.value.settings
        validateMonitorSettings(settings.baseUrl, settings.apiKey)?.let { throw IllegalStateException(it) }
        val workflows = api.workflows(settings)
        _ui.update { it.copy(workflows = workflows) }
    }

    fun loadExecutions(workflowId: String) = load {
        val executions = api.executions(_ui.value.settings, workflowId = workflowId)
        _ui.update { it.copy(executions = executions) }
    }

    private fun load(block: suspend () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            runCatching { block() }.fold(
                onSuccess = { _ui.update { it.copy(loading = false) } },
                onFailure = { failure ->
                    _ui.update {
                        it.copy(loading = false, error = failure.message ?: "n8n could not be reached.")
                    }
                },
            )
        }
    }
}
