package com.example.models.statistics

import kotlinx.serialization.Serializable
import java.time.LocalDateTime


@Serializable
data class UserStatistics(
    val userId: String,
    val totalTasksCompleted: Int = 0,
    val totalTasksAttempted: Int = 0,
    val totalTimeSpentSeconds: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val coursesEnrolled: Int = 0,
    val coursesCompleted: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastActivityDate: String? = null, // YYYY-MM-DD
    val updatedAt: String = LocalDateTime.now().toString()
)

