package com.example.models.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class CourseActivityInPeriod(
    val courseId: String,
    val courseName: String,
    val tasksCompletedInPeriod: Int,
    val newEnrollments: Int,
    val completionsInPeriod: Int
)