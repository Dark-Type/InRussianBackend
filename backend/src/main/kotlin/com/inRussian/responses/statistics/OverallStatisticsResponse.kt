package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class OverallStatisticsResponse(
    val totalStudents: Long?,
    val averageTimeSpentSeconds: Long?,
    val averageProgressPercentage: Double?
)