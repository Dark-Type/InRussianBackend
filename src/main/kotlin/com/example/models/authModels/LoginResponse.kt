package com.example.models.authModels

import com.example.models.user.User
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val user: User,
    val profile: UserProfileInfo? = null,
    val token: String,
    val expiresAt: String
)