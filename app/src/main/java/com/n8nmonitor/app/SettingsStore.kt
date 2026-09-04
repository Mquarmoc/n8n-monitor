package com.n8nmonitor.app

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class MonitorSettings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val pollMinutes: Int = 15,
    val notificationsEnabled: Boolean = false,
)

@Suppress("DEPRECATION")
class SettingsStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "monitor_settings",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): MonitorSettings = MonitorSettings(
        baseUrl = preferences.getString(BASE_URL, "").orEmpty(),
        apiKey = preferences.getString(API_KEY, "").orEmpty(),
        pollMinutes = preferences.getInt(POLL_MINUTES, 15),
        notificationsEnabled = preferences.getBoolean(NOTIFICATIONS, false),
    )

    fun save(settings: MonitorSettings) {
        val previous = load()
        preferences.edit {
            putString(BASE_URL, settings.baseUrl)
            putString(API_KEY, settings.apiKey)
            putInt(POLL_MINUTES, settings.pollMinutes)
            putBoolean(NOTIFICATIONS, settings.notificationsEnabled)
            if (previous.baseUrl != settings.baseUrl || previous.apiKey != settings.apiKey) {
                remove(INITIALIZED)
                remove(SEEN_ERRORS)
            }
        }
    }

    fun isInitialized(): Boolean = preferences.getBoolean(INITIALIZED, false)

    fun seenErrorIds(): Set<String> =
        preferences.getStringSet(SEEN_ERRORS, emptySet())?.toSet().orEmpty()

    fun recordSeenErrorIds(ids: Iterable<String>) {
        preferences.edit {
            putStringSet(SEEN_ERRORS, ids.distinct().take(MAX_SEEN).toSet())
            putBoolean(INITIALIZED, true)
        }
    }

    private companion object {
        const val BASE_URL = "base_url"
        const val API_KEY = "api_key"
        const val POLL_MINUTES = "poll_minutes"
        const val NOTIFICATIONS = "notifications"
        const val INITIALIZED = "initialized"
        const val SEEN_ERRORS = "seen_errors"
        const val MAX_SEEN = 100
    }
}
