package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class CourseAverageProgressResponse(
    val courseId: String,
    val courseName: String,
    val averageProgressPercentage: Double,
    val totalStudents: Int,
    val completedStudents: Int
)