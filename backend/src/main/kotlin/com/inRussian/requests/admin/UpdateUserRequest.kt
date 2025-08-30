package com.inRussian.requests.admin

import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val phone: String? = null,
    val role: UserRole? = null,
    val systemLanguage: SystemLanguage? = null,
    val avatarId: String? = null,
    val status: UserStatus = UserStatus.ACTIVE
)