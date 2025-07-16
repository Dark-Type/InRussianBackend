package com.inRussian.models.courses

import kotlinx.serialization.Serializable

@Serializable
data class ThemeWithTaskCount(
    val theme: Theme,
    val taskCount: Int,
    val completedTasks: Int = 0
)