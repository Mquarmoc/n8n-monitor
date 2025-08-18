package com.example.n8nmonitor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.n8nmonitor.R
import com.example.n8nmonitor.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val pollInterval by viewModel.pollIntervalMinutes.collectAsStateWithLifecycle(initialValue = 15)
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val isNotificationEnabled by viewModel.isNotificationEnabled.collectAsStateWithLifecycle(initialValue = true)
    val settingsState by viewModel.state.collectAsStateWithLifecycle()

    var baseUrlInput by remember(baseUrl) { mutableStateOf(baseUrl ?: "") }
    var apiKeyInput by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    // Update input fields when stored values change
    LaunchedEffect(baseUrl) {
        baseUrlInput = baseUrl ?: ""
    }
    
    LaunchedEffect(apiKey) {
        apiKeyInput = apiKey ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Connection Settings
            SettingsSection(title = "Connection") {
                OutlinedTextField(
                    value = baseUrlInput ?: "",
                    onValueChange = { 
                        baseUrlInput = it
                        viewModel.updateBaseUrl(it)
                    },
                    label = { Text("n8n Base URL") },
                    placeholder = { Text("https://your-n8n-instance.com") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = settingsState.baseUrlError != null,
                    supportingText = settingsState.baseUrlError?.let { error ->
                        { Text(text = error, color = MaterialTheme.colorScheme.error) }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKeyInput ?: "",
                    onValueChange = { 
                        apiKeyInput = it
                        viewModel.updateApiKey(it)
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your n8n API key") },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.Close else Icons.Default.Info,
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = settingsState.apiKeyError != null,
                    supportingText = settingsState.apiKeyError?.let { error ->
                        { Text(text = error, color = MaterialTheme.colorScheme.error) }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Validation is handled in real-time by updateBaseUrl/updateApiKey
                        },
                        modifier = Modifier.weight(1f),
                        enabled = settingsState.baseUrlError == null && 
                                 settingsState.apiKeyError == null &&
                                 baseUrlInput.isNotBlank() && 
                                 apiKeyInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                    
                    OutlinedButton(
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.weight(1f),
                        enabled = !settingsState.isTestingConnection &&
                                 settingsState.baseUrlError == null && 
                                 settingsState.apiKeyError == null &&
                                 baseUrlInput.isNotBlank() && 
                                 apiKeyInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test")
                    }
                }
                
                // Test connection result
                settingsState.testConnectionResult?.let { result ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.contains("successful")) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (result.contains("successful")) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearTestResult() }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (result.contains("successful")) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Monitoring Settings
            SettingsSection(title = "Monitoring") {
                Text(
                    text = "Poll Interval",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = pollInterval.toFloat(),
                    onValueChange = { viewModel.updatePollInterval(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = pluralStringResource(
                        R.plurals.settings_poll_interval_minutes,
                        pollInterval,
                        pollInterval
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Background Notifications",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Receive notifications for failed executions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isNotificationEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Appearance Settings
            SettingsSection(title = "Appearance") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dark Mode",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Use dark theme",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Security Settings
            SettingsSection(title = "Security") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Biometric Authentication",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Require biometric to access API key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = false,
                        onCheckedChange = { /* Biometric authentication disabled */ },
                        enabled = false
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Data Management
            SettingsSection(title = "Data Management") {
                OutlinedButton(
                    onClick = { viewModel.clearSettings() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Settings")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        content()
    }
}