package com.example.n8nmonitor.data.exceptions

/**
 * Exception thrown when API configuration is missing or invalid
 */
class ApiConfigurationException(
    message: String,
    val isBaseUrlMissing: Boolean = false,
    val isApiKeyMissing: Boolean = false,
    val isBaseUrlInvalid: Boolean = false
) : Exception(message) {
    
    companion object {
        fun missingConfiguration(): ApiConfigurationException {
            return ApiConfigurationException(
                "API configuration is incomplete. Please configure your n8n server URL and API key in Settings.",
                isBaseUrlMissing = true,
                isApiKeyMissing = true
            )
        }
        
        fun missingBaseUrl(): ApiConfigurationException {
            return ApiConfigurationException(
                "n8n server URL is not configured. Please set your server URL in Settings.",
                isBaseUrlMissing = true
            )
        }
        
        fun missingApiKey(): ApiConfigurationException {
            return ApiConfigurationException(
                "API key is not configured. Please set your API key in Settings.",
                isApiKeyMissing = true
            )
        }
        
        fun invalidBaseUrl(url: String): ApiConfigurationException {
            return ApiConfigurationException(
                "Invalid server URL format: '$url'. Please check your server URL in Settings.",
                isBaseUrlInvalid = true
            )
        }
    }
}