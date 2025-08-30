package com.inRussian.requests.users

import kotlinx.serialization.Serializable

@Serializable
data class CreateStaffProfileRequest(
    val name: String,
    val surname: String,
    val patronymic: String? = null
)