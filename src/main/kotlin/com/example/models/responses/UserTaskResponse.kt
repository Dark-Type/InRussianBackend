package com.example.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserTaskResponse(
    val taskId: String,
    val userAnswer: kotlinx.serialization.json.JsonElement,
    val timeTaken: Int? = null
)