package com.example.models.admin

import kotlinx.serialization.Serializable

@Serializable
data class UpdateStaffProfileRequest(
    val name: String? = null,
    val surname: String? = null,
    val patronymic: String? = null
)