package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
data class StudentCourseProgress(
    val courseId: String,
    val courseName: String,
    val progressPercentage: Double,
    val timeSpentMinutes: Int,
    val tasksCompleted: Int,
    val tasksTotal: Int,
    val lastActivityAt: String?,
    val isCompleted: Boolean = false
)