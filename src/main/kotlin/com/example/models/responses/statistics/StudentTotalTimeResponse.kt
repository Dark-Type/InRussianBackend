package com.example.models.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StudentTotalTimeResponse(
    val userId: String,
    val userName: String,
    val totalTimeSpentMinutes: Int,
    val coursesEnrolled: Int,
    val coursesCompleted: Int,
    val averageTimePerCourse: Double
)