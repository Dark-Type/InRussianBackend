package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
data class CourseTimeDistribution(
    val averageCompletionTimeHours: Double,
    val medianCompletionTimeHours: Double,
    val fastestCompletionTimeHours: Double,
    val slowestCompletionTimeHours: Double,
    val timeRanges: List<TimeRangeDistribution>
)