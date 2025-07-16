package com.example.models.user

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT, EXPERT, CONTENT_MODERATOR, ADMIN
}