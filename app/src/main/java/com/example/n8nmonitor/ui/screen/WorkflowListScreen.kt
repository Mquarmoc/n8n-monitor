package com.example.n8nmonitor.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.n8nmonitor.data.database.WorkflowEntity
import com.example.n8nmonitor.ui.viewmodel.WorkflowListViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowListScreen(
    onWorkflowClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: WorkflowListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(state.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("n8n Workflows") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.refreshWorkflows() },
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
                        onRetry = { viewModel.loadWorkflows() },
                        onDismiss = { viewModel.clearError() }
                    )
                }
                state.workflows.isEmpty() -> {
                    EmptyContent(
                        onRefresh = { viewModel.refreshWorkflows() }
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.workflows) { workflow ->
                            WorkflowCard(
                                workflow = workflow,
                                onClick = { onWorkflowClick(workflow.id) },
                                onBookmarkToggle = { isBookmarked ->
                                    viewModel.toggleBookmark(workflow.id, isBookmarked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowCard(
    workflow: WorkflowEntity,
    onClick: () -> Unit,
    onBookmarkToggle: (Boolean) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                        text = workflow.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ID: ${workflow.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Active status chip
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
                        
                        // Last execution status
                        workflow.lastExecutionStatus?.let { status ->
                            AssistChip(
                                onClick = { },
                                label = { Text(status) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when (status.lowercase()) {
                                        "success" -> MaterialTheme.colorScheme.primaryContainer
                                        "failed" -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Last modified
                    Text(
                        text = "Updated: ${formatDate(workflow.updatedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { onBookmarkToggle(!workflow.isBookmarked) }
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = if (workflow.isBookmarked) "Remove bookmark" else "Add bookmark",
                        tint = if (workflow.isBookmarked) MaterialTheme.colorScheme.secondary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
            
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun EmptyContent(
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Workflows",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "No active workflows found. Pull to refresh to try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
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