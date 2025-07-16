package com.example.models.expertRelated

import kotlinx.serialization.Serializable

@Serializable
data class StudentActivitySummary(
    val userId: String,
    val userName: String,
    val email: String,
    val totalTasksCompleted: Int,
    val totalTimeSpentHours: Double,
    val coursesEnrolled: Int,
    val coursesCompleted: Int,
    val lastActivityAt: String?,
    val averageProgressPercentage: Double
)