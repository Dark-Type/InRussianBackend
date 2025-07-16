package com.example.models.authModels

import kotlinx.serialization.Serializable

@Serializable
data class PasswordRecoveryRequest(
    val email: String
)

