package com.example.models.badge

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserBadge(
    val userId: String,
    val badgeId: String,
    val earnedAt: String = LocalDateTime.now().toString(),
    val courseId: String? = null,
    val themeId: String? = null
)