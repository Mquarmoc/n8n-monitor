package com.example.n8nmonitor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.n8nmonitor.data.database.ExecutionEntity
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.ui.viewmodel.WorkflowDetailViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDetailScreen(
    workflowId: String,
    onBackClick: () -> Unit,
    viewModel: WorkflowDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(state.isRefreshing)
    var showStopDialog by remember { mutableStateOf<ExecutionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.workflow?.name ?: "Workflow Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshExecutions() },
            modifier = Modifier.padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> {
                    ErrorContent(
                        message = state.error ?: "Unknown error occurred",
                        onRetry = { viewModel.loadWorkflowDetails() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Workflow header
                        state.workflow?.let { workflow ->
                            item {
                                WorkflowHeader(workflow = workflow)
                            }
                        }
                        
                        // Executions
                        if (state.executions.isEmpty()) {
                            item {
                                EmptyExecutionsContent(
                                    onRefresh = { viewModel.refreshExecutions() }
                                )
                            }
                        } else {
                            items(state.executions) { execution ->
                                ExecutionCard(
                                    execution = execution,
                                    onStopClick = { showStopDialog = execution }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Stop execution dialog
    showStopDialog?.let { execution ->
        AlertDialog(
            onDismissRequest = { showStopDialog = null },
            title = { Text("Stop Execution") },
            text = { 
                Text("Are you sure you want to stop this execution? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopExecution(execution.id)
                        showStopDialog = null
                    }
                ) {
                    Text("Stop")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WorkflowHeader(workflow: WorkflowEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = workflow.name,
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(if (workflow.active) "Active" else "Inactive")
                    },
                    leadingIcon = {
                        Icon(
                            if (workflow.active) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (workflow.active) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (workflow.active) 
                            MaterialTheme.colorScheme.primaryContainer
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                )
                
                workflow.tags?.let { tags ->
                    if (tags.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Tags: $tags") }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Last updated: ${formatDate(workflow.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionCard(
    execution: ExecutionEntity,
    onStopClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Execution ${execution.id.take(8)}...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(execution.status) },
                            leadingIcon = {
                                Icon(
                                    when (execution.status.lowercase()) {
                                        "success" -> Icons.Default.CheckCircle
                                        "failed" -> Icons.Default.Warning
                                        "running" -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = when (execution.status.lowercase()) {
                                        "success" -> MaterialTheme.colorScheme.primary
                                        "failed" -> MaterialTheme.colorScheme.error
                                        "running" -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (execution.status.lowercase()) {
                                    "success" -> MaterialTheme.colorScheme.primaryContainer
                                    "failed" -> MaterialTheme.colorScheme.errorContainer
                                    "running" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        )
                        
                        execution.duration?.let { duration ->
                            AssistChip(
                                onClick = { },
                                label = { Text("${duration}ms") }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    execution.startTime?.let { startTime ->
                        Text(
                            text = "Started: ${formatDate(startTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (execution.status.lowercase() == "running") {
                    IconButton(
                        onClick = onStopClick
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Stop execution",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Expanded content - could show more details here
                Text(
                    text = "Execution ID: ${execution.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                execution.endTime?.let { endTime ->
                    Text(
                        text = "Ended: ${formatDate(endTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}



@Composable
fun EmptyExecutionsContent(
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Executions",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No executions found for this workflow. Pull to refresh to try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}