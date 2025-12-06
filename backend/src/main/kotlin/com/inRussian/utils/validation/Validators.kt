package com.inRussian.utils.validation

object Validators {
    fun email(value: String?, field: String = "email") =
        if (value.isNullOrBlank() || !value.contains('@'))
            FieldError(field, "invalid_email", "Email is invalid") else null

    fun nonEmpty(value: String?, field: String, max: Int? = null) =
        when {
            value.isNullOrBlank() -> FieldError(field, "required", "$field is required")
            max != null && value.length > max -> FieldError(field, "too_long", "$field too long")
            else -> null
        }

    fun password(value: String?, min: Int = 8, max: Int = 255) =
        when {
            value.isNullOrBlank() -> FieldError("password", "required", "Password is required")
            value.length < min -> FieldError("password", "too_short", "Password too short")
            value.length > max -> FieldError("password", "too_long", "Password too long")
            else -> null
        }

    fun phone(value: String?, field: String = "phone") =
        if (value != null && value.length !in 5..50)
            FieldError(field, "invalid_phone", "Phone length invalid") else null
}