package com.example.n8nmonitor.ui.viewmodel

import com.example.n8nmonitor.data.settings.SettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var settingsDataStore: SettingsDataStore
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsDataStore = mockk(relaxed = true)
        
        // Setup default flow values
        coEvery { settingsDataStore.baseUrl } returns flowOf("https://default.example.com")
        coEvery { settingsDataStore.apiKey } returns flowOf("default-api-key")
        coEvery { settingsDataStore.pollIntervalMinutes } returns flowOf(15)
        coEvery { settingsDataStore.isDarkMode } returns flowOf(false)
        coEvery { settingsDataStore.isBiometricEnabled } returns flowOf(false)
        coEvery { settingsDataStore.isNotificationEnabled } returns flowOf(true)
        
        viewModel = SettingsViewModel(settingsDataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        // Then
        assertEquals("", viewModel.state.value.baseUrl)
        assertEquals("", viewModel.state.value.apiKey)
        assertEquals(15, viewModel.state.value.pollIntervalMinutes)
        assertFalse(viewModel.state.value.isDarkMode)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isNotificationEnabled)
    }

    @Test
    fun `setBaseUrl updates state and calls datastore`() = runTest {
        // Given
        val testUrl = "https://test.example.com"
        
        // When
        viewModel.setBaseUrl(testUrl)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(testUrl, viewModel.state.value.baseUrl)
        coVerify { settingsDataStore.setBaseUrl(testUrl) }
    }

    @Test
    fun `setApiKey updates state and calls datastore`() = runTest {
        // Given
        val testKey = "test-api-key-123"
        
        // When
        viewModel.setApiKey(testKey)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(testKey, viewModel.state.value.apiKey)
        coVerify { settingsDataStore.setApiKey(testKey) }
    }

    @Test
    fun `setPollInterval updates state and calls datastore`() = runTest {
        // Given
        val testInterval = 30
        
        // When
        viewModel.setPollInterval(testInterval)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(testInterval, viewModel.state.value.pollIntervalMinutes)
        coVerify { settingsDataStore.setPollIntervalMinutes(testInterval) }
    }

    @Test
    fun `toggleDarkMode updates state and calls datastore`() = runTest {
        // When - Enable dark mode
        viewModel.toggleDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isDarkMode)
        coVerify { settingsDataStore.setDarkMode(true) }
        
        // When - Disable dark mode
        viewModel.toggleDarkMode(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isDarkMode)
        coVerify { settingsDataStore.setDarkMode(false) }
    }

    @Test
    fun `toggleBiometric updates state and calls datastore`() = runTest {
        // When - Enable biometric
        viewModel.toggleBiometric(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isBiometricEnabled)
        coVerify { settingsDataStore.setBiometricEnabled(true) }
        
        // When - Disable biometric
        viewModel.toggleBiometric(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isBiometricEnabled)
        coVerify { settingsDataStore.setBiometricEnabled(false) }
    }

    @Test
    fun `toggleNotifications updates state and calls datastore`() = runTest {
        // When - Disable notifications
        viewModel.toggleNotifications(false)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.state.value.isNotificationEnabled)
        coVerify { settingsDataStore.setNotificationEnabled(false) }
        
        // When - Enable notifications
        viewModel.toggleNotifications(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.state.value.isNotificationEnabled)
        coVerify { settingsDataStore.setNotificationEnabled(true) }
    }

    @Test
    fun `clearSettings resets state and calls datastore`() = runTest {
        // Given - Set some non-default values
        viewModel.setBaseUrl("https://test.example.com")
        viewModel.setApiKey("test-key")
        viewModel.toggleDarkMode(true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When
        viewModel.clearSettings()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals("", viewModel.state.value.baseUrl)
        assertEquals("", viewModel.state.value.apiKey)
        assertEquals(15, viewModel.state.value.pollIntervalMinutes)
        assertFalse(viewModel.state.value.isDarkMode)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isNotificationEnabled)
        coVerify { settingsDataStore.clearSettings() }
    }
}