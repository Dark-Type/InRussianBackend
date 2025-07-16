package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
data class CourseOverviewStats(
    val courseId: String,
    val courseName: String,
    val studentsEnrolled: Int,
    val studentsCompleted: Int,
    val averageProgress: Double,
    val averageTimePerStudentHours: Double,
    val completionRate: Double
)

