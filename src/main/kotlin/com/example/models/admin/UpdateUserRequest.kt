package com.example.models.admin

import com.example.models.user.SystemLanguage
import com.example.models.user.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val phone: String? = null,
    val role: UserRole? = null,
    val systemLanguage: SystemLanguage? = null,
    val avatarId: String? = null
)