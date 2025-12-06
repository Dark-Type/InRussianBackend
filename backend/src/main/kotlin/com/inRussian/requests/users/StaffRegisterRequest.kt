package com.inRussian.requests.users

import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlinx.serialization.Serializable

@Serializable
data class StaffRegisterRequest(
    @field:Email("{email.invalid}")
    val email: String,
    @field:Size(min = 6, message = "{password.min}")
    @field:Pattern(regexp = ".*\\d.*", message = "{password.digit}")
    @field:Pattern(regexp = ".*[!@#$%^&*()_].*", message = "{password.special}")
    val password: String,
    @field:Pattern(regexp = "^\\+?\\d{1,15}$", message = "{phone.invalid}")
    val phone: String? = null,
    val role: UserRole,
    val name: String,
    val surname: String,
    val patronymic: String? = null,
    val systemLanguage: SystemLanguage = SystemLanguage.RUSSIAN
)