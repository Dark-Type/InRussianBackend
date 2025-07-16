package com.inRussian.models.users

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT, EXPERT, CONTENT_MODERATOR, ADMIN
}