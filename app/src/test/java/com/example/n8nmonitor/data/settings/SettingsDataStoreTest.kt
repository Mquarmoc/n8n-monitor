package com.example.n8nmonitor.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testDefaultValues() = runBlocking {
        // Test default values
        assertEquals(null, settingsDataStore.baseUrl.first())
        assertEquals(null, settingsDataStore.apiKey.first())
        assertEquals(15, settingsDataStore.pollIntervalMinutes.first())
        assertFalse(settingsDataStore.isDarkMode.first())
        assertFalse(settingsDataStore.isBiometricEnabled.first())
        assertTrue(settingsDataStore.isNotificationEnabled.first())
        assertEquals(0L, settingsDataStore.lastSyncTime.first())
    }

    @Test
    fun testSetBaseUrl() = runBlocking {
        // Given
        val testUrl = "https://test-n8n.example.com"
        
        // When
        settingsDataStore.setBaseUrl(testUrl)
        
        // Then
        assertEquals(testUrl, settingsDataStore.baseUrl.first())
    }

    @Test
    fun testSetApiKey() = runBlocking {
        // Given
        val testKey = "test-api-key-123"
        
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
        assertTrue(settingsDataStore.isDarkMode.first())
        
        // When
        settingsDataStore.setDarkMode(false)
        
        // Then
        assertFalse(settingsDataStore.isDarkMode.first())
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
        
        // Then - Verify defaults are restored
        assertEquals(null, settingsDataStore.baseUrl.first())
        assertEquals(null, settingsDataStore.apiKey.first())
        assertEquals(15, settingsDataStore.pollIntervalMinutes.first())
        assertFalse(settingsDataStore.isDarkMode.first())
    }
}