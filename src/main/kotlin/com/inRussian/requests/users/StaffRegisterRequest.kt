package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class StaffRegisterRequest(
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: UserRole, // EXPERT or CONTENT_MODERATOR
    val systemLanguage: SystemLanguage = SystemLanguage.ENGLISH
)