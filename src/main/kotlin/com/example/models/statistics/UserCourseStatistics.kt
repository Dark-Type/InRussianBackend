package com.example.models.statistics

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class UserCourseStatistics(
    val userId: String,
    val courseId: String,
    val tasksCompleted: Int = 0,
    val tasksAttempted: Int = 0,
    val tasksTotal: Int = 0,
    val timeSpentSeconds: Int = 0,
    val correctAnswers: Int = 0,
    val progressPercentage: Double = 0.0,
    val startedAt: String? = null,
    val lastActivityAt: String? = null,
    val completedAt: String? = null,
    val updatedAt: String = LocalDateTime.now().toString()
)