package com.example.n8nmonitor.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.settings.SettingsDataStore

/**
 * Test worker factory for creating MonitoringWorker instances with mocked dependencies
 */
class TestWorkerFactory(
    private val repository: N8nRepository,
    private val settingsDataStore: SettingsDataStore
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            MonitoringWorker::class.java.name -> {
                MonitoringWorker(
                    appContext,
                    workerParameters,
                    repository,
                    settingsDataStore
                )
            }
            else -> null
        }
    }
}