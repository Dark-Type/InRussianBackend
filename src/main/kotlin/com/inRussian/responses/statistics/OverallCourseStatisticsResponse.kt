package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class OverallCourseStatisticsResponse(
    val studentsCount: Long?,
    val averageTimeSpentSeconds: Long?,
    val averageProgressPercentage: Double?
)
