package com.example.models.user

import kotlinx.serialization.Serializable

@Serializable
data class StaffProfile(
    val userId: String,
    val name: String,
    val surname: String,
    val patronymic: String? = null
)
