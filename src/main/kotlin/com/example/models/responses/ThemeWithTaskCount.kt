package com.example.models.responses

import com.example.models.course.Theme
import kotlinx.serialization.Serializable

@Serializable
data class ThemeWithTaskCount(
    val theme: Theme,
    val taskCount: Int,
    val completedTasks: Int = 0
)