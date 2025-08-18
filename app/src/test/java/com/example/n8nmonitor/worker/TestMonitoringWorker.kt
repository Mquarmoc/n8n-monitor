package com.example.n8nmonitor.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import androidx.work.CoroutineWorker
import com.example.n8nmonitor.R
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.example.n8nmonitor.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Test version of MonitoringWorker without Hilt dependency injection
 */
class TestMonitoringWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repository: N8nRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID_FAILURES = "n8n_failures"
        private const val CHANNEL_ID_GENERAL = "n8n_general"
        private const val NOTIFICATION_ID_FAILURES = 1001
        private const val NOTIFICATION_ID_GENERAL = 1002
    }

    override suspend fun doWork(): Result {
        try {
            // Check if notifications are enabled
            val isNotificationEnabled = settingsDataStore.isNotificationEnabled.first()
            if (!isNotificationEnabled) {
                return Result.success()
            }

            // Get failed executions from the last hour
            val oneHourAgo = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .format(Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)))

            val failedExecutions = repository.getFailedExecutionsSince(oneHourAgo)

            if (failedExecutions.isNotEmpty()) {
                createNotificationChannels()
                sendFailureNotification(failedExecutions)
            }

            // Clean up stale data
            repository.cleanupStaleData()

            return Result.success()
        } catch (e: Exception) {
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Failures channel
            val failuresChannel = NotificationChannel(
                CHANNEL_ID_FAILURES,
                "n8n Failures",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for failed n8n workflow executions"
                enableVibration(true)
            }

            // General channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "n8n General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General n8n monitoring notifications"
            }

            notificationManager.createNotificationChannels(listOf(failuresChannel, generalChannel))
        }
    }

    private fun sendFailureNotification(failedExecutions: List<com.example.n8nmonitor.data.database.ExecutionEntity>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Group executions by workflow
        val executionsByWorkflow = failedExecutions.groupBy { it.workflowId }

        if (executionsByWorkflow.size == 1) {
            // Single workflow failure
            val (workflowId, executions) = executionsByWorkflow.entries.first()
            val execution = executions.first()
            
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data = android.net.Uri.parse("n8nmonitor://workflow/$workflowId")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_FAILURES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Workflow Execution Failed")
                .setContentText("Execution ${execution.id.take(8)}... failed")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_FAILURES, notification)
        } else {
            // Multiple workflow failures
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID_FAILURES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Multiple Workflow Failures")
                .setContentText("${executionsByWorkflow.size} workflows have failed executions")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_FAILURES, notification)
        }
    }
}