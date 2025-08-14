package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import kotlinx.serialization.Serializable

import jakarta.validation.constraints.*

@Serializable
data class StudentRegisterRequest(
    @field:Email("{email.invalid}")
    val email: String,
    @field:Size(min = 6, message = "{password.min}")
    @field:Pattern(regexp = ".*\\d.*", message = "{password.digit}")
    @field:Pattern(regexp = ".*[!@#$%^&*()_].*", message = "{password.special}")
    val password: String,
    @field:Pattern("^\\+[1-9][0-9]{7,14}$", message = "{phone.invalid}")
    val phone: String? = null,
    val systemLanguage: SystemLanguage = SystemLanguage.ENGLISH
)