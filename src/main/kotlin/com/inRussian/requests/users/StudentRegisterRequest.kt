package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import kotlinx.serialization.Serializable

@Serializable
data class StudentRegisterRequest(
    val email: String,
    val password: String,
    val phone: String? = null,
    val systemLanguage: SystemLanguage = SystemLanguage.ENGLISH
)