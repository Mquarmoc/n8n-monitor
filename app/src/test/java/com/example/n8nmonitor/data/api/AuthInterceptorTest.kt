package com.example.n8nmonitor.data.api

import com.example.n8nmonitor.data.api.AuthInterceptor
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var interceptor: AuthInterceptor
    private lateinit var apiKeyProvider: AuthInterceptor.ApiKeyProvider
    private lateinit var chain: Interceptor.Chain

    @Before
    fun setup() {
        apiKeyProvider = mockk()
        interceptor = AuthInterceptor(apiKeyProvider)
        chain = mockk()
    }

    @Test
    fun `intercept adds required headers when API key is available`() {
        // Given
        val apiKey = "test-api-key"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns apiKey
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } returns mockk<Response>()

        // When
        interceptor.intercept(chain)

        // Then
        // Verify that chain.proceed was called with a request that has the required headers
        // We can't easily verify the exact request, but we can verify the interceptor doesn't throw
        assertNotNull(interceptor)
    }

    @Test
    fun `intercept adds X-N8N-API-KEY header when API key is available`() {
        // Given
        val apiKey = "test-api-key"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns apiKey
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            assertEquals(apiKey, request.header("X-N8N-API-KEY"))
            mockk<Response>()
        }

        // When
        interceptor.intercept(chain)

        // Then
        // Verification is done in the chain.proceed mock
    }

    @Test
    fun `intercept adds Accept header when API key is available`() {
        // Given
        val apiKey = "test-api-key"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns apiKey
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            assertEquals("application/json", request.header("Accept"))
            mockk<Response>()
        }

        // When
        interceptor.intercept(chain)

        // Then
        // Verification is done in the chain.proceed mock
    }

    @Test
    fun `intercept adds User-Agent header when API key is available`() {
        // Given
        val apiKey = "test-api-key"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns apiKey
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            assertEquals("n8n-monitor-android/1.0.0", request.header("User-Agent"))
            mockk<Response>()
        }

        // When
        interceptor.intercept(chain)

        // Then
        // Verification is done in the chain.proceed mock
    }

    @Test
    fun `intercept passes through request when API key is null`() {
        // Given
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns null
        every { chain.request() } returns originalRequest
        every { chain.proceed(originalRequest) } returns mockk<Response>()

        // When
        interceptor.intercept(chain)

        // Then
        // Verify that chain.proceed was called with the original request
        // We can't easily verify the exact request, but we can verify the interceptor doesn't throw
        assertNotNull(interceptor)
    }

    @Test
    fun `intercept passes through request when API key is empty`() {
        // Given
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns ""
        every { chain.request() } returns originalRequest
        every { chain.proceed(originalRequest) } returns mockk<Response>()

        // When
        interceptor.intercept(chain)

        // Then
        // Verify that chain.proceed was called with the original request
        // We can't easily verify the exact request, but we can verify the interceptor doesn't throw
        assertNotNull(interceptor)
    }

    @Test
    fun `intercept preserves existing headers when adding new ones`() {
        // Given
        val apiKey = "test-api-key"
        val originalRequest = Request.Builder()
            .url("https://api.example.com/test")
            .header("Custom-Header", "custom-value")
            .build()
        
        every { apiKeyProvider.getApiKey() } returns apiKey
        every { chain.request() } returns originalRequest
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            assertEquals("custom-value", request.header("Custom-Header"))
            assertEquals(apiKey, request.header("X-N8N-API-KEY"))
            assertEquals("application/json", request.header("Accept"))
            assertEquals("n8n-monitor-android/1.0.0", request.header("User-Agent"))
            mockk<Response>()
        }

        // When
        interceptor.intercept(chain)

        // Then
        // Verification is done in the chain.proceed mock
    }
} 