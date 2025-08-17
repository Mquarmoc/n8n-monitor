package com.example.n8nmonitor.data.api

import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = runBlocking { apiKeyProvider.getApiKey() }
        
        return if (apiKey != null && apiKey.isNotBlank()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("X-N8N-API-KEY", apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", "n8n-monitor-android/1.0.0")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

interface ApiKeyProvider {
    suspend fun getApiKey(): String?
    suspend fun getBaseUrl(): String?
}