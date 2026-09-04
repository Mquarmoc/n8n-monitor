package com.n8nmonitor.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL

data class Workflow(
    val id: String,
    val name: String,
    val active: Boolean,
    val updatedAt: String?,
)

data class Execution(
    val id: String,
    val workflowId: String,
    val status: String,
    val startedAt: String?,
    val stoppedAt: String?,
)

internal fun validateMonitorSettings(baseUrl: String, apiKey: String): String? {
    if (apiKey.isBlank()) return "Enter an n8n API key."
    val uri = runCatching { URI(baseUrl.trim()) }.getOrNull()
        ?: return "Enter a valid n8n URL."
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) {
        return "The n8n URL must use HTTPS."
    }
    if (uri.userInfo != null) return "Do not put credentials in the n8n URL."
    if (uri.rawQuery != null || uri.rawFragment != null) {
        return "Use the n8n root URL without a query or fragment."
    }
    return null
}

class N8nApi {
    suspend fun workflows(settings: MonitorSettings, limit: Int = 250): List<Workflow> =
        parseWorkflows(get(settings, "workflows", mapOf("limit" to limit.toString())))

    suspend fun executions(
        settings: MonitorSettings,
        workflowId: String? = null,
        status: String? = null,
        limit: Int = 100,
    ): List<Execution> {
        val query = buildMap {
            put("limit", limit.toString())
            workflowId?.takeIf(String::isNotBlank)?.let { put("workflowId", it) }
            status?.takeIf(String::isNotBlank)?.let { put("status", it) }
        }
        return parseExecutions(get(settings, "executions", query))
    }

    private suspend fun get(
        settings: MonitorSettings,
        path: String,
        query: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        validateMonitorSettings(settings.baseUrl, settings.apiKey)?.let { throw IOException(it) }
        val encodedQuery = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        val url = URL("${settings.baseUrl.trim().trimEnd('/')}/api/v1/$path?$encodedQuery")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "n8n-monitor-android/0.1.0")
            connection.setRequestProperty("X-N8N-API-KEY", settings.apiKey.trim())

            val statusCode = connection.responseCode
            val body = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (statusCode !in 200..299) throw IOException(httpError(statusCode, body))
            body
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun httpError(statusCode: Int, body: String): String {
        val detail = runCatching { JSONObject(body).optString("message") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
        return when (statusCode) {
            401 -> "The n8n API key was rejected."
            403 -> "The API key cannot access this n8n resource."
            404 -> "The n8n public API was not found at this URL."
            else -> detail ?: "n8n returned HTTP $statusCode."
        }
    }
}

internal fun parseWorkflows(json: String): List<Workflow> {
    val data = JSONObject(json).getJSONArray("data")
    return List(data.length()) { index ->
        val item = data.getJSONObject(index)
        Workflow(
            id = item.get("id").toString(),
            name = item.getString("name"),
            active = item.optBoolean("active"),
            updatedAt = item.optNullableString("updatedAt"),
        )
    }
}

internal fun parseExecutions(json: String): List<Execution> {
    val data = JSONObject(json).getJSONArray("data")
    return List(data.length()) { index ->
        val item = data.getJSONObject(index)
        Execution(
            id = item.get("id").toString(),
            workflowId = item.get("workflowId").toString(),
            status = item.getString("status"),
            startedAt = item.optNullableString("startedAt"),
            stoppedAt = item.optNullableString("stoppedAt"),
        )
    }
}

private fun JSONObject.optNullableString(name: String): String? =
    if (isNull(name)) null else optString(name).takeIf(String::isNotBlank)
