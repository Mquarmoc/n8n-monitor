package com.example.n8nmonitor.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.n8nmonitor.data.repository.N8nRepository
import com.example.n8nmonitor.data.settings.SettingsDataStore

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
                // Create MonitoringWorker with manual dependency injection
                // We'll use reflection to bypass the @AssistedInject constructor
                try {
                    val constructor = MonitoringWorker::class.java.getDeclaredConstructor(
                        Context::class.java,
                        WorkerParameters::class.java,
                        N8nRepository::class.java,
                        SettingsDataStore::class.java
                    )
                    constructor.isAccessible = true
                    constructor.newInstance(
                        appContext,
                        workerParameters,
                        repository,
                        settingsDataStore
                    ) as MonitoringWorker
                } catch (e: Exception) {
                    // Fallback to TestMonitoringWorker if reflection fails
                    TestMonitoringWorker(
                        appContext,
                        workerParameters,
                        repository,
                        settingsDataStore
                    )
                }
            }
            else -> null
        }
    }
}