package com.example.n8nmonitor.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExecutionDto(
    val id: String,
    val status: String,
    @Json(name = "workflowId") val workflowId: String,
    val start: String? = null,
    val end: String? = null,
    val nodes: List<ExecutionNodeDto>? = null,
    val timing: ExecutionTimingDto? = null
)

@JsonClass(generateAdapter = true)
data class ExecutionNodeDto(
    val id: String,
    val name: String,
    val type: String,
    val status: String,
    val error: String? = null,
    val data: Map<String, Any>? = null,
    val start: String? = null,
    val end: String? = null
)

@JsonClass(generateAdapter = true)
data class ExecutionTimingDto(
    val start: String? = null,
    val end: String? = null,
    val duration: Long? = null
)

@JsonClass(generateAdapter = true)
data class ExecutionsResponseDto(
    val results: List<ExecutionDto>,
    @Json(name = "nextCursor") val nextCursor: String? = null
) 