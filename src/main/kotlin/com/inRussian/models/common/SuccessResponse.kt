package com.inRussian.models.common

import kotlinx.serialization.Serializable

@Serializable
data class SuccessResponse<T>(
    val success: Boolean = true,
    val data: T,
    val message: String? = null,
    val timestamp: String = java.time.LocalDateTime.now().toString()
)