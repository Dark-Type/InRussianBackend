package com.example.models.validation

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val success: Boolean = false,
    val message: String,
    val errors: List<ValidationError> = emptyList(),
    val timestamp: String = java.time.LocalDateTime.now().toString()
)