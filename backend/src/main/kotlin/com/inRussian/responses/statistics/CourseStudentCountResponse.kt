package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class CourseStudentCountResponse(
    val courseId: String,
    val courseName: String,
    val studentCount: Int
)