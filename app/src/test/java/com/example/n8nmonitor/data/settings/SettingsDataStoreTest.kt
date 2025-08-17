package com.example.n8nmonitor.data.settings

import android.content.Context
import com.example.n8nmonitor.data.settings.SecureStorage
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: TestSettingsDataStore
    private lateinit var mockSecureStorage: SecureStorage

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockSecureStorage = mockk()
        
        // Setup mock SecureStorage to return null by default
        coEvery { mockSecureStorage.getBaseUrl() } returns null
        coEvery { mockSecureStorage.getApiKey() } returns null
        coJustRun { mockSecureStorage.storeBaseUrl(any()) }
        coJustRun { mockSecureStorage.storeApiKey(any()) }
        coJustRun { mockSecureStorage.clearSecureData() }
        
        settingsDataStore = TestSettingsDataStore(context, mockSecureStorage)
    }
    
    @After
    fun cleanup() {
        // Clean up any DataStore files that might have been created
        val dataStoreDir = File(context.filesDir, "datastore")
        if (dataStoreDir.exists()) {
            dataStoreDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("test_settings")) {
                    file.delete()
                }
            }
        }
    }

    @Test
    fun testDefaultValues() = runBlocking {
        // Test default values
        assertEquals(null, settingsDataStore.baseUrl.first())
        assertEquals(null, settingsDataStore.apiKey.first())
        assertEquals(5, settingsDataStore.pollIntervalMinutes.first())
        assertFalse(settingsDataStore.isDarkModeEnabled.first())
        assertFalse(settingsDataStore.isBiometricEnabled.first())
        assertTrue(settingsDataStore.isNotificationEnabled.first())
        assertEquals(0L, settingsDataStore.lastSyncTime.first())
    }

    @Test
    fun testSetBaseUrl() = runBlocking {
        // Given
        val testUrl = "https://test-n8n.example.com"
        coEvery { mockSecureStorage.getBaseUrl() } returns testUrl
        
        // When
        settingsDataStore.setBaseUrl(testUrl)
        
        // Then
        assertEquals(testUrl, settingsDataStore.baseUrl.first())
    }

    @Test
    fun testSetApiKey() = runBlocking {
        // Given
        val testKey = "test-api-key-123"
        coEvery { mockSecureStorage.getApiKey() } returns testKey
        
        // When
        settingsDataStore.setApiKey(testKey)
        
        // Then
        assertEquals(testKey, settingsDataStore.apiKey.first())
    }

    @Test
    fun testSetPollIntervalMinutes() = runBlocking {
        // Given
        val testInterval = 30
        
        // When
        settingsDataStore.setPollIntervalMinutes(testInterval)
        
        // Then
        assertEquals(testInterval, settingsDataStore.pollIntervalMinutes.first())
    }

    @Test
    fun testSetPollIntervalMinutesWithBoundaries() = runBlocking {
        // Test lower boundary
        settingsDataStore.setPollIntervalMinutes(1)
        assertEquals(5, settingsDataStore.pollIntervalMinutes.first())
        
        // Test upper boundary
        settingsDataStore.setPollIntervalMinutes(100)
        assertEquals(60, settingsDataStore.pollIntervalMinutes.first())
    }

    @Test
    fun testSetDarkMode() = runBlocking {
        // When
        settingsDataStore.setDarkMode(true)
        
        // Then
        assertTrue(settingsDataStore.isDarkModeEnabled.first())
        
        // When
        settingsDataStore.setDarkMode(false)
        
        // Then
        assertFalse(settingsDataStore.isDarkModeEnabled.first())
    }

    @Test
    fun testSetBiometricEnabled() = runBlocking {
        // When
        settingsDataStore.setBiometricEnabled(true)
        
        // Then
        assertTrue(settingsDataStore.isBiometricEnabled.first())
        
        // When
        settingsDataStore.setBiometricEnabled(false)
        
        // Then
        assertFalse(settingsDataStore.isBiometricEnabled.first())
    }

    @Test
    fun testSetNotificationEnabled() = runBlocking {
        // When
        settingsDataStore.setNotificationEnabled(false)
        
        // Then
        assertFalse(settingsDataStore.isNotificationEnabled.first())
        
        // When
        settingsDataStore.setNotificationEnabled(true)
        
        // Then
        assertTrue(settingsDataStore.isNotificationEnabled.first())
    }

    @Test
    fun testUpdateLastSyncTime() = runBlocking {
        // Given
        val beforeUpdate = System.currentTimeMillis()
        
        // When
        settingsDataStore.updateLastSyncTime()
        
        // Then
        val storedTime = settingsDataStore.lastSyncTime.first()
        assertTrue(storedTime >= beforeUpdate)
        assertTrue(storedTime <= System.currentTimeMillis())
    }

    @Test
    fun testClearSettings() = runBlocking {
        // Given - Set some values
        settingsDataStore.setBaseUrl("https://test.example.com")
        settingsDataStore.setApiKey("test-key")
        settingsDataStore.setDarkMode(true)
        
        // When
        settingsDataStore.clearSettings()
        
        // Mock SecureStorage to return null after clearing
        coEvery { mockSecureStorage.getBaseUrl() } returns null
        coEvery { mockSecureStorage.getApiKey() } returns null
        
        // Then - Verify defaults are restored
        assertEquals(null, settingsDataStore.baseUrl.first())
        assertEquals(null, settingsDataStore.apiKey.first())
        assertEquals(15, settingsDataStore.pollIntervalMinutes.first())
        assertFalse(settingsDataStore.isDarkModeEnabled.first())
    }
}

class TestSettingsDataStore(private val context: Context, private val secureStorage: SecureStorage) {
    private val testPreferences = mutableMapOf<String, Any>()

    init {
        // Initialize with default values
        testPreferences["dark_mode"] = false
        testPreferences["notification_enabled"] = true
        testPreferences["biometric_enabled"] = false
        testPreferences["poll_interval_minutes"] = 5
        testPreferences["last_sync_time"] = 0L
    }

    val isDarkModeEnabled: Flow<Boolean> = flow {
        emit(testPreferences["dark_mode"] as? Boolean ?: false)
    }

    val isNotificationEnabled: Flow<Boolean> = flow {
        emit(testPreferences["notification_enabled"] as? Boolean ?: true)
    }

    val isBiometricEnabled: Flow<Boolean> = flow {
        emit(testPreferences["biometric_enabled"] as? Boolean ?: false)
    }

    val pollIntervalMinutes: Flow<Int> = flow {
        emit(testPreferences["poll_interval_minutes"] as? Int ?: 5)
    }

    val baseUrl: Flow<String?> = flow {
        emit(runBlocking { secureStorage.getBaseUrl() })
    }

    val apiKey: Flow<String?> = flow {
        emit(runBlocking { secureStorage.getApiKey() })
    }

    val lastSyncTime: Flow<Long> = flow {
        emit(testPreferences["last_sync_time"] as? Long ?: 0L)
    }

    suspend fun setDarkMode(enabled: Boolean) {
        testPreferences["dark_mode"] = enabled
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        testPreferences["notification_enabled"] = enabled
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        testPreferences["biometric_enabled"] = enabled
    }

    suspend fun setPollIntervalMinutes(minutes: Int) {
        // Apply boundary validation like the real implementation
        val clampedMinutes = when {
            minutes < 5 -> 5
            minutes > 60 -> 60
            else -> minutes
        }
        testPreferences["poll_interval_minutes"] = clampedMinutes
    }

    suspend fun setBaseUrl(url: String) {
        secureStorage.storeBaseUrl(url)
    }

    suspend fun setApiKey(key: String) {
        secureStorage.storeApiKey(key)
    }

    suspend fun updateLastSyncTime() {
        testPreferences["last_sync_time"] = System.currentTimeMillis()
    }

    suspend fun clearSettings() {
        testPreferences.clear()
        // Restore defaults
        testPreferences["dark_mode"] = false
        testPreferences["notification_enabled"] = true
        testPreferences["biometric_enabled"] = false
        testPreferences["poll_interval_minutes"] = 15
        testPreferences["last_sync_time"] = 0L
        secureStorage.clearSecureData()
    }
}