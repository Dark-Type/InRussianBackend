package com.example.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserProgressResponse(
    val courseId: String,
    val courseName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val currentSection: String? = null,
    val currentTheme: String? = null,
    val progressPercentage: Double
)