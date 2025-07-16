package com.example.models.validation

import kotlinx.serialization.Serializable

@Serializable
data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)

