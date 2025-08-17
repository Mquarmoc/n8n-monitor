package com.example.n8nmonitor.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage
) {

    private object PreferencesKeys {
        // Removed BASE_URL and API_KEY - now stored securely
        val POLL_INTERVAL_MINUTES = intPreferencesKey("poll_interval_minutes")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val baseUrl: Flow<String?> = flow {
        emit(secureStorage.getBaseUrl())
    }

    val apiKey: Flow<String?> = flow {
        emit(secureStorage.getApiKey())
    }

    val pollIntervalMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.POLL_INTERVAL_MINUTES] ?: 15
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: false
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
    }

    val isNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_ENABLED] ?: true
    }

    val lastSyncTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SYNC_TIME] ?: 0L
    }

    suspend fun setBaseUrl(url: String) {
        secureStorage.storeBaseUrl(url)
    }

    suspend fun setApiKey(key: String) {
        secureStorage.storeApiKey(key)
    }

    suspend fun setPollIntervalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.POLL_INTERVAL_MINUTES] = minutes.coerceIn(5, 60)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun updateLastSyncTime() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun clearSettings() {
        // Clear secure data
        secureStorage.clearSecureData()
        
        // Clear regular preferences
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}