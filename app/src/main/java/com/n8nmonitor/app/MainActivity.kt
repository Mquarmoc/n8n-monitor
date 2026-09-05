package com.n8nmonitor.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deepWorkflowId = intent?.data?.takeIf { it.host == "workflow" }?.pathSegments?.firstOrNull()
        setContent {
            N8nMonitorTheme {
                App(viewModel, deepWorkflowId)
            }
        }
    }
}

@Composable
private fun N8nMonitorTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF82C976))
    } else {
        lightColorScheme(primary = Color(0xFF347A2B))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun App(viewModel: MainViewModel, deepWorkflowId: String?) {
    val ui by viewModel.ui.collectAsState()
    val navigation = rememberNavController()
    val configured = validateMonitorSettings(ui.settings.baseUrl, ui.settings.apiKey) == null
    val startDestination = remember { if (configured) "workflows" else "settings" }

    LaunchedEffect(Unit) {
        if (configured) viewModel.loadWorkflows()
    }
    LaunchedEffect(deepWorkflowId) {
        if (configured && !deepWorkflowId.isNullOrBlank()) {
            navigation.navigate("workflow/${Uri.encode(deepWorkflowId)}")
        }
    }

    Surface(Modifier.fillMaxSize()) {
        NavHost(
            navController = navigation,
            startDestination = startDestination,
        ) {
            composable("settings") {
                SettingsScreen(
                    ui = ui,
                    onTest = viewModel::testConnection,
                    onSave = { url, key, minutes, notifications ->
                        if (viewModel.saveSettings(url, key, minutes, notifications)) {
                            viewModel.loadWorkflows()
                            if (!navigation.popBackStack("workflows", inclusive = false)) {
                                navigation.navigate("workflows") {
                                    popUpTo("settings") { inclusive = true }
                                }
                            }
                        }
                    },
                )
            }
            composable("workflows") {
                WorkflowsScreen(
                    ui = ui,
                    onRefresh = viewModel::loadWorkflows,
                    onSettings = { navigation.navigate("settings") },
                    onWorkflow = { workflowId ->
                        navigation.navigate("workflow/${Uri.encode(workflowId)}")
                    },
                )
            }
            composable(
                route = "workflow/{workflowId}",
                arguments = listOf(navArgument("workflowId") { type = NavType.StringType }),
            ) { entry ->
                val workflowId = entry.arguments?.getString("workflowId").orEmpty()
                LaunchedEffect(workflowId) { viewModel.loadExecutions(workflowId) }
                ExecutionsScreen(
                    workflowId = workflowId,
                    ui = ui,
                    onBack = { navigation.popBackStack() },
                    onRefresh = { viewModel.loadExecutions(workflowId) },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    ui: MainUiState,
    onTest: (String, String) -> Unit,
    onSave: (String, String, Int, Boolean) -> Unit,
) {
    var baseUrl by remember(ui.settings.baseUrl) { mutableStateOf(ui.settings.baseUrl) }
    var apiKey by remember(ui.settings.apiKey) { mutableStateOf(ui.settings.apiKey) }
    var pollMinutes by remember(ui.settings.pollMinutes) { mutableIntStateOf(ui.settings.pollMinutes) }
    var notifications by remember(ui.settings.notificationsEnabled) {
        mutableStateOf(ui.settings.notificationsEnabled)
    }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifications = it
    }

    Page(Modifier.verticalScroll(rememberScrollState())) {
        Text("n8n Monitor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Connect one n8n instance. The API key stays encrypted on this device.")
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("n8n HTTPS URL") },
            placeholder = { Text("https://n8n.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Text("Background check interval", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60).forEach { minutes ->
                FilterChip(
                    selected = pollMinutes == minutes,
                    onClick = { pollMinutes = minutes },
                    label = { Text("$minutes min") },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Failure notifications", fontWeight = FontWeight.SemiBold)
                Text("The first check establishes a baseline.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                modifier = Modifier.semantics { contentDescription = "Failure notifications" },
                checked = notifications,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notifications = enabled
                    }
                },
            )
        }
        Feedback(ui)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onTest(baseUrl, apiKey) },
                enabled = !ui.testing,
            ) {
                Text(if (ui.testing) "Testing…" else "Test connection")
            }
            Button(onClick = { onSave(baseUrl, apiKey, pollMinutes, notifications) }) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun WorkflowsScreen(
    ui: MainUiState,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onWorkflow: (String) -> Unit,
) {
    Page {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Workflows", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(onClick = onSettings) { Text("Settings") }
            }
        }
        Feedback(ui)
        if (ui.loading && ui.workflows.isEmpty()) CircularProgressIndicator()
        if (!ui.loading && ui.workflows.isEmpty() && ui.error == null) {
            Text("No workflows found.")
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.workflows, key = Workflow::id) { workflow ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onWorkflow(workflow.id) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(workflow.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (workflow.active) "Active" else "Inactive",
                            color = if (workflow.active) MaterialTheme.colorScheme.primary else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        workflow.updatedAt?.let { Text("Updated $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionsScreen(
    workflowId: String,
    ui: MainUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Page {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = onRefresh, enabled = !ui.loading) { Text("Refresh") }
        }
        Text("Recent executions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Workflow $workflowId", style = MaterialTheme.typography.bodySmall)
        Feedback(ui)
        if (ui.loading && ui.executions.isEmpty()) CircularProgressIndicator()
        if (!ui.loading && ui.executions.isEmpty() && ui.error == null) {
            Text("No recent executions found.")
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.executions, key = Execution::id) { execution ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(execution.status.replaceFirstChar(Char::uppercase), fontWeight = FontWeight.SemiBold)
                        Text("Execution ${execution.id}", style = MaterialTheme.typography.bodySmall)
                        execution.startedAt?.let { Text("Started $it", style = MaterialTheme.typography.bodySmall) }
                        execution.stoppedAt?.let { Text("Stopped $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Feedback(ui: MainUiState) {
    ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    ui.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().then(modifier).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
