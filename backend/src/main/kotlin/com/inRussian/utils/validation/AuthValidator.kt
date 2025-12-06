package com.inRussian.utils.validation

import com.inRussian.models.users.UserRole

object AuthValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^\\+?[1-9]\\d{6,14}$")
    private const val MIN_PASSWORD_LENGTH = 8

    fun validateEmail(email: String): List<FieldError> {
        val errors = mutableListOf<FieldError>()
        if (email.isBlank()) {
            errors.add(FieldError("email", "REQUIRED", "Email is required"))
        } else if (!EMAIL_REGEX.matches(email)) {
            errors.add(FieldError("email", "INVALID_FORMAT", "Invalid email format"))
        }
        return errors
    }

    fun validatePassword(password: String): List<FieldError> {
        val errors = mutableListOf<FieldError>()
        if (password.isBlank()) {
            errors.add(FieldError("password", "REQUIRED", "Password is required"))
        } else {
            if (password.length < MIN_PASSWORD_LENGTH) {
                errors.add(FieldError("password", "TOO_SHORT", "Password must be at least $MIN_PASSWORD_LENGTH characters"))
            }
            if (!password.any { it.isUpperCase() }) {
                errors.add(FieldError("password", "MISSING_UPPERCASE", "Password must contain at least one uppercase letter"))
            }
            if (!password.any { it.isLowerCase() }) {
                errors.add(FieldError("password", "MISSING_LOWERCASE", "Password must contain at least one lowercase letter"))
            }
            if (!password.any { it.isDigit() }) {
                errors.add(FieldError("password", "MISSING_DIGIT", "Password must contain at least one digit"))
            }
        }
        return errors
    }

    fun validatePhone(phone: String?): List<FieldError> {
        if (phone.isNullOrBlank()) return emptyList()
        return if (!PHONE_REGEX.matches(phone)) {
            listOf(FieldError("phone", "INVALID_FORMAT", "Invalid phone format"))
        } else emptyList()
    }

    fun validateStaffRole(role: UserRole): List<FieldError> {
        val allowedRoles = setOf(UserRole.EXPERT, UserRole.CONTENT_MODERATOR, UserRole.ADMIN)
        return if (role !in allowedRoles) {
            listOf(FieldError("role", "INVALID_ROLE", "Role must be one of: ${allowedRoles.joinToString()}"))
        } else emptyList()
    }

    fun validateRefreshToken(token: String): List<FieldError> {
        return if (token.isBlank()) {
            listOf(FieldError("refreshToken", "REQUIRED", "Refresh token is required"))
        } else emptyList()
    }
}