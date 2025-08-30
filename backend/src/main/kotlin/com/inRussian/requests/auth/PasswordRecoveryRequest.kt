package com.inRussian.requests.auth

import kotlinx.serialization.Serializable

@Serializable
data class PasswordRecoveryRequest(
    val email: String
)

