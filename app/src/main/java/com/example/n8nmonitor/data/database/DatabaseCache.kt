package com.example.n8nmonitor.data.database

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache for frequently accessed database data
 * Implements LRU eviction policy and thread-safe operations
 */
@Singleton
class DatabaseCache @Inject constructor() {
    
    private val mutex = Mutex()
    private val maxCacheSize = 100
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutes
    
    // Workflow cache
    private val workflowCache = mutableMapOf<String, CacheEntry<WorkflowEntity>>()
    private val workflowListCache = mutableMapOf<String, CacheEntry<List<WorkflowEntity>>>()
    
    // Execution cache
    private val executionCache = mutableMapOf<String, CacheEntry<ExecutionEntity>>()
    private val executionListCache = mutableMapOf<String, CacheEntry<List<ExecutionEntity>>>()
    
    // Statistics cache
    private val statsCache = mutableMapOf<String, CacheEntry<List<ExecutionStats>>>()
    
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 1
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > 5 * 60 * 1000L
        fun touch() { accessCount++ }
    }
    
    // Workflow caching methods
    suspend fun getWorkflow(id: String): WorkflowEntity? = mutex.withLock {
        workflowCache[id]?.let { entry ->
            if (!entry.isExpired()) {
                entry.touch()
                entry.data
            } else {
                workflowCache.remove(id)
                null
            }
        }
    }
    
    suspend fun putWorkflow(id: String, workflow: WorkflowEntity) = mutex.withLock {
        evictIfNeeded(workflowCache)
        workflowCache[id] = CacheEntry(workflow)
    }
    
    suspend fun getWorkflowList(key: String): List<WorkflowEntity>? = mutex.withLock {
        workflowListCache[key]?.let { entry ->
            if (!entry.isExpired()) {
                entry.touch()
                entry.data
            } else {
                workflowListCache.remove(key)
                null
            }
        }
    }
    
    suspend fun putWorkflowList(key: String, workflows: List<WorkflowEntity>) = mutex.withLock {
        evictIfNeeded(workflowListCache)
        workflowListCache[key] = CacheEntry(workflows)
    }
    
    // Execution caching methods
    suspend fun getExecution(id: String): ExecutionEntity? = mutex.withLock {
        executionCache[id]?.let { entry ->
            if (!entry.isExpired()) {
                entry.touch()
                entry.data
            } else {
                executionCache.remove(id)
                null
            }
        }
    }
    
    suspend fun putExecution(id: String, execution: ExecutionEntity) = mutex.withLock {
        evictIfNeeded(executionCache)
        executionCache[id] = CacheEntry(execution)
    }
    
    suspend fun getExecutionList(key: String): List<ExecutionEntity>? = mutex.withLock {
        executionListCache[key]?.let { entry ->
            if (!entry.isExpired()) {
                entry.touch()
                entry.data
            } else {
                executionListCache.remove(key)
                null
            }
        }
    }
    
    suspend fun putExecutionList(key: String, executions: List<ExecutionEntity>) = mutex.withLock {
        evictIfNeeded(executionListCache)
        executionListCache[key] = CacheEntry(executions)
    }
    
    // Statistics caching methods
    suspend fun getStats(key: String): List<ExecutionStats>? = mutex.withLock {
        statsCache[key]?.let { entry ->
            if (!entry.isExpired()) {
                entry.touch()
                entry.data
            } else {
                statsCache.remove(key)
                null
            }
        }
    }
    
    suspend fun putStats(key: String, stats: List<ExecutionStats>) = mutex.withLock {
        evictIfNeeded(statsCache)
        statsCache[key] = CacheEntry(stats)
    }
    
    // Cache management methods
    suspend fun invalidateWorkflow(id: String) = mutex.withLock {
        workflowCache.remove(id)
        // Also invalidate related list caches
        workflowListCache.clear()
    }
    
    suspend fun invalidateExecution(id: String) = mutex.withLock {
        executionCache.remove(id)
        // Also invalidate related list caches
        executionListCache.clear()
    }
    
    suspend fun invalidateAll() = mutex.withLock {
        workflowCache.clear()
        workflowListCache.clear()
        executionCache.clear()
        executionListCache.clear()
        statsCache.clear()
    }
    
    fun cleanupExpired() {
        synchronized(this) {
            workflowCache.entries.removeAll { (_, entry) ->
                entry.isExpired()
            }
            executionCache.entries.removeAll { (_, entry) ->
                entry.isExpired()
            }
            statsCache.entries.removeAll { (_, entry) ->
                entry.isExpired()
            }
        }
    }
    
    // LRU eviction policy
    private fun <T> evictIfNeeded(cache: MutableMap<String, CacheEntry<T>>) {
        if (cache.size >= maxCacheSize) {
            // Remove least recently used entry
            val lruKey = cache.entries.minByOrNull { it.value.accessCount }?.key
            lruKey?.let { cache.remove(it) }
        }
    }
    
    // Cache statistics for monitoring
    suspend fun getCacheStats(): CacheStats = mutex.withLock {
        CacheStats(
            workflowCacheSize = workflowCache.size,
            workflowListCacheSize = workflowListCache.size,
            executionCacheSize = executionCache.size,
            executionListCacheSize = executionListCache.size,
            statsCacheSize = statsCache.size,
            totalCacheSize = workflowCache.size + workflowListCache.size + 
                           executionCache.size + executionListCache.size + statsCache.size
        )
    }
    
    data class CacheStats(
        val workflowCacheSize: Int,
        val workflowListCacheSize: Int,
        val executionCacheSize: Int,
        val executionListCacheSize: Int,
        val statsCacheSize: Int,
        val totalCacheSize: Int
    )
}