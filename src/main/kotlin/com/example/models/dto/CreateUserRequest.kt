package com.example.models.dto

import com.example.models.user.SystemLanguage
import com.example.models.user.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val email: String,
    val phone: String? = null,
    val role: UserRole,
    val systemLanguage: SystemLanguage,
    val avatarId: String? = null
)


