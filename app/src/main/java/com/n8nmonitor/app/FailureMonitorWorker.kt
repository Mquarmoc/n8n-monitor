package com.n8nmonitor.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

internal fun unseenExecutions(
    executions: List<Execution>,
    seenIds: Set<String>,
): List<Execution> = executions.filterNot { it.id in seenIds }

class FailureMonitorWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val store = runCatching { SettingsStore(applicationContext) }
            .getOrElse { return Result.failure() }
        val settings = runCatching { store.load() }.getOrElse { return Result.failure() }
        if (!settings.notificationsEnabled ||
            validateMonitorSettings(settings.baseUrl, settings.apiKey) != null
        ) return Result.success()

        return try {
            val errors = N8nApi().executions(settings, status = "error", limit = 100)
            val previousIds = store.seenErrorIds()
            if (store.isInitialized()) {
                unseenExecutions(errors, previousIds).takeIf(List<Execution>::isNotEmpty)?.let(::notify)
            }
            store.recordSeenErrorIds(errors.map(Execution::id) + previousIds)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun notify(errors: List<Execution>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Workflow failures", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val first = errors.first()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "n8nmonitor://workflow/${Uri.encode(first.workflowId)}".toUri()
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (errors.size == 1) {
            "Workflow ${first.workflowId} has a new failed execution."
        } else {
            "${errors.size} workflow executions failed since the last check."
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("n8n failure detected")
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "n8n-failure-monitor"
        private const val CHANNEL_ID = "workflow-failures"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context, settings: MonitorSettings) {
            val workManager = WorkManager.getInstance(context)
            if (!settings.notificationsEnabled ||
                validateMonitorSettings(settings.baseUrl, settings.apiKey) != null
            ) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<FailureMonitorWorker>(
                settings.pollMinutes.coerceAtLeast(15).toLong(),
                TimeUnit.MINUTES,
            ).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            ).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
