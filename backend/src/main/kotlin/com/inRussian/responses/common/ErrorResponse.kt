package com.inRussian.responses.common

import kotlinx.serialization.Serializable

@Serializable
data class ValidationErrorDetail(
    val field: String,
    val code: String,
    val message: String
)

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val code: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val details: List<ValidationErrorDetail>? = null
)