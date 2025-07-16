package com.example.models.expertRelated

import kotlinx.serialization.Serializable

@Serializable
data class SystemOverviewStats(
    val totalCourses: Int,
    val totalTasks: Int,
    val averageTasksPerCourse: Double,
    val averageTimePerTask: Double,
    val overallSuccessRate: Double
)