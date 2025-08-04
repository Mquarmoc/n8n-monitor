package com.example.n8nmonitor.data.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkflowDto(
    val id: String,
    val name: String,
    val active: Boolean,
    @Json(name = "updatedAt") val updatedAt: String,
    val tags: List<String>? = null,
    val nodes: List<NodeDto>? = null,
    val triggers: List<TriggerDto>? = null
)

@JsonClass(generateAdapter = true)
data class NodeDto(
    val id: String,
    val name: String,
    val type: String,
    val position: List<Int>? = null
)

@JsonClass(generateAdapter = true)
data class TriggerDto(
    val id: String,
    val name: String,
    val type: String
) 