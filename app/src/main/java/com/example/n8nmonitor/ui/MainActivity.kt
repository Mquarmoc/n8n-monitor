package com.example.n8nmonitor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.n8nmonitor.ui.screen.SettingsScreen
import com.example.n8nmonitor.ui.screen.StartupScreen
import com.example.n8nmonitor.ui.screen.WorkflowDetailScreen
import com.example.n8nmonitor.ui.screen.WorkflowListScreen
import com.example.n8nmonitor.ui.theme.N8nMonitorTheme
import com.example.n8nmonitor.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState(initial = false)
            
            N8nMonitorTheme(
                darkTheme = isDarkMode || isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    N8nMonitorApp()
                }
            }
        }
    }
}

@Composable
fun N8nMonitorApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "startup"
    ) {
        composable("startup") {
            StartupScreen(
                onNavigateToWorkflows = {
                    navController.navigate("workflows") {
                        popUpTo("startup") { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        popUpTo("startup") { inclusive = true }
                    }
                }
            )
        }
        
        composable("workflows") {
            WorkflowListScreen(
                onWorkflowClick = { workflowId ->
                    navController.navigate("workflow/$workflowId")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("workflow/{workflowId}") { backStackEntry ->
            val workflowId = backStackEntry.arguments?.getString("workflowId")
            requireNotNull(workflowId) { "workflowId parameter is required" }
            
            WorkflowDetailScreen(
                workflowId = workflowId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}