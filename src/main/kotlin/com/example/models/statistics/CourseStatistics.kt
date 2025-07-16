package com.example.models.statistics

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class CourseStatistics(
    val courseId: String,
    val studentsEnrolled: Int = 0,
    val studentsCompleted: Int = 0,
    val studentsActiveLast7Days: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalTimeSpentSeconds: Int = 0,
    val totalAttempts: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val averageCompletionTimeSeconds: Int = 0,
    val updatedAt: String = LocalDateTime.now().toString()
)