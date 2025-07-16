package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val email: String,
    val phone: String? = null,
    val role: UserRole,
    val systemLanguage: SystemLanguage,
    val avatarId: String? = null
)


