package com.example.models.authModels

import kotlinx.serialization.Serializable

@Serializable
data class PasswordRecoveryCode(
    val email: String,
    val code: String,
    val expiresAt: String,
    val isUsed: Boolean = false,
    val createdAt: String = java.time.LocalDateTime.now().toString()
)