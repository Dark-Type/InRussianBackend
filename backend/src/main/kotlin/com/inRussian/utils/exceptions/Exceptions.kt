package com.inRussian.utils.exceptions


sealed class AppException(override val message: String, val code: String) : RuntimeException(message)

class UserAlreadyExistsException(email: String) :
    AppException("User with email $email already exists", "USER_ALREADY_EXISTS")

class UserNotFoundException(identifier: String) :
    AppException("User not found: $identifier", "USER_NOT_FOUND")

class InvalidCredentialsException :
    AppException("Invalid email or password", "INVALID_CREDENTIALS")

class AccountSuspendedException :
    AppException("Account is suspended", "ACCOUNT_SUSPENDED")

class AccountDeactivatedException :
    AppException("Account is deactivated", "ACCOUNT_DEACTIVATED")

class InvalidRoleException(role: String) :
    AppException("Invalid role for registration: $role", "INVALID_ROLE")

class AdminAlreadyExistsException :
    AppException("Admin already exists", "ADMIN_ALREADY_EXISTS")

class ConfigurationException(field: String) :
    AppException("$field not configured", "CONFIGURATION_ERROR")

class InvalidTokenException(reason: String = "Invalid or expired token") :
    AppException(reason, "INVALID_TOKEN")

class DatabaseException(operation: String, cause: Throwable? = null) :
    AppException("Database error during $operation", "DATABASE_ERROR") {
    init {
        cause?.let { initCause(it) }
    }
}
