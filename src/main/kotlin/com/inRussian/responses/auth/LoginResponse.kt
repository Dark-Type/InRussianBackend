package com.inRussian.responses.auth

import com.inRussian.models.auth.UserProfileInfo
import com.inRussian.models.users.User
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val user: User,
    val profile: UserProfileInfo? = null,
    val token: String,
    val expiresAt: String
)