package com.inRussian.requests.users

import kotlinx.serialization.Serializable

@Serializable
data class UpdateStaffProfileRequest(
    val name: String? = null,
    val surname: String? = null,
    val patronymic: String? = null
)