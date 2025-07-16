package com.example.models.authModels

import kotlinx.serialization.Serializable

@Serializable
data class VerifyRecoveryCodeRequest(
    val email: String,
    val code: String,
    val newPassword: String
)