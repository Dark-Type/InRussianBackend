package com.inRussian.responses.common

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val code: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

