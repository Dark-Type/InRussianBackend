package com.inRussian.models.users

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val systemLanguage: String,
    val status: String
)