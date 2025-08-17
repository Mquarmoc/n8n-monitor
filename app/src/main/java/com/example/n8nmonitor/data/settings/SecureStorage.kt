package com.example.n8nmonitor.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey: MasterKey by lazy {
        try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true) // Use hardware security module if available
                .setUserAuthenticationRequired(false) // Can be enabled for additional security
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                .build()
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "StrongBox not available, falling back to software-backed key", e)
            // Fallback to software-backed key if StrongBox is not available
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        var retryCount = 0
        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                return@lazy EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: GeneralSecurityException) {
                Log.w(TAG, "Failed to create encrypted preferences, attempt ${retryCount + 1}", e)
                retryCount++
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    throw SecurityException("Failed to initialize secure storage after $MAX_RETRY_ATTEMPTS attempts", e)
                }
                // Clear corrupted preferences and retry
                try {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().clear().apply()
                } catch (clearException: Exception) {
                    Log.w(TAG, "Failed to clear corrupted preferences", clearException)
                }
            }
        }
        throw SecurityException("Unable to create secure storage")
    }
    
    companion object {
        private const val TAG = "SecureStorage"
        private const val PREFS_NAME = "n8n_secure_settings"
        private const val KEY_API_KEY = "encrypted_api_key"
        private const val KEY_BASE_URL = "encrypted_base_url"
        private const val KEY_LAST_ACCESS = "last_access_time"
        
        // Security constants
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val ACCESS_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }
    
    suspend fun storeApiKey(apiKey: String) = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(apiKey.length >= 8) { "API key must be at least 8 characters long" }
        
        try {
            val currentTime = System.currentTimeMillis()
            encryptedPrefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putLong(KEY_LAST_ACCESS, currentTime)
                .apply()
            Log.d(TAG, "API key stored securely")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store API key securely", e)
            throw SecurityException("Failed to store API key securely: ${e.message}", e)
        }
    }
    
    suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        try {
            val lastAccess = encryptedPrefs.getLong(KEY_LAST_ACCESS, 0L)
            val currentTime = System.currentTimeMillis()
            
            // Check if access has timed out (optional security feature)
            if (lastAccess > 0 && (currentTime - lastAccess) > ACCESS_TIMEOUT_MS) {
                Log.w(TAG, "Access timeout exceeded, clearing secure data")
                clearSecureData()
                return@withContext null
            }
            
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            
            // Update last access time if key exists
            if (apiKey != null) {
                encryptedPrefs.edit()
                    .putLong(KEY_LAST_ACCESS, currentTime)
                    .apply()
            }
            
            apiKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve API key securely", e)
            throw SecurityException("Failed to retrieve API key securely: ${e.message}", e)
        }
    }
    
    suspend fun storeBaseUrl(baseUrl: String) = withContext(Dispatchers.IO) {
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) { 
            "Base URL must start with http:// or https://" 
        }
        
        try {
            val currentTime = System.currentTimeMillis()
            encryptedPrefs.edit()
                .putString(KEY_BASE_URL, baseUrl.trimEnd('/'))
                .putLong(KEY_LAST_ACCESS, currentTime)
                .apply()
            Log.d(TAG, "Base URL stored securely")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store base URL securely", e)
            throw SecurityException("Failed to store base URL securely: ${e.message}", e)
        }
    }
    
    suspend fun getBaseUrl(): String? = withContext(Dispatchers.IO) {
        try {
            val lastAccess = encryptedPrefs.getLong(KEY_LAST_ACCESS, 0L)
            val currentTime = System.currentTimeMillis()
            
            // Check if access has timed out (optional security feature)
            if (lastAccess > 0 && (currentTime - lastAccess) > ACCESS_TIMEOUT_MS) {
                Log.w(TAG, "Access timeout exceeded, clearing secure data")
                clearSecureData()
                return@withContext null
            }
            
            val baseUrl = encryptedPrefs.getString(KEY_BASE_URL, null)
            
            // Update last access time if URL exists
            if (baseUrl != null) {
                encryptedPrefs.edit()
                    .putLong(KEY_LAST_ACCESS, currentTime)
                    .apply()
            }
            
            baseUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve base URL securely", e)
            throw SecurityException("Failed to retrieve base URL securely: ${e.message}", e)
        }
    }
    
    suspend fun clearSecureData() = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit()
                .remove(KEY_API_KEY)
                .remove(KEY_BASE_URL)
                .remove(KEY_LAST_ACCESS)
                .apply()
            Log.d(TAG, "Secure data cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear secure data", e)
            throw SecurityException("Failed to clear secure data: ${e.message}", e)
        }
    }
    
    suspend fun hasSecureData(): Boolean = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.contains(KEY_API_KEY) || encryptedPrefs.contains(KEY_BASE_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check secure data existence", e)
            false
        }
    }
    
    /**
     * Validates the integrity of stored secure data
     */
    suspend fun validateSecureData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
            val baseUrl = encryptedPrefs.getString(KEY_BASE_URL, null)
            
            val isApiKeyValid = apiKey?.isNotBlank() == true && apiKey.length >= 8
            val isBaseUrlValid = baseUrl?.isNotBlank() == true && 
                (baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))
            
            isApiKeyValid && isBaseUrlValid
        } catch (e: Exception) {
            Log.w(TAG, "Failed to validate secure data", e)
            false
        }
    }
    
    /**
     * Gets the last access time for security monitoring
     */
    suspend fun getLastAccessTime(): Long = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.getLong(KEY_LAST_ACCESS, 0L)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get last access time", e)
            0L
        }
    }
}