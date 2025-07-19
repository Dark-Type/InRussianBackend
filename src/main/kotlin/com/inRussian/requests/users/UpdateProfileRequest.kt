package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val phone: String? = null,
    val systemLanguage: SystemLanguage? = null,
    val avatarId: String? = null
)
