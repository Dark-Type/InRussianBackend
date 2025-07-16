package com.example.models.progress

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserCourseEnrollment(
    val userId: String,
    val courseId: String,
    val enrolledAt: String = LocalDateTime.now().toString(),
    val completedAt: String? = null,
    val currentSectionId: String? = null,
    val currentThemeId: String? = null
)

