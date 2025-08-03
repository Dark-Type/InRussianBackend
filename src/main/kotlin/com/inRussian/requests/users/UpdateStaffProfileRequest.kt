package com.inRussian.requests.users

import kotlinx.serialization.Serializable

@Serializable
data class UpdateStaffProfileRequest(
    val name: String? = null,
    val surname: String? = null,
    val patronymic: String? = null,
    val passwordHash: String? = null,
    val systemLanguage: String? = null,
    val phone: String? = null,
    val avatarId: String? = null
)