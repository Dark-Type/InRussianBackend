package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StudentTimeSpentResponse(
    val userId: String,
    val userName: String,
    val courseId: String,
    val courseName: String,
    val timeSpentMinutes: Int
)