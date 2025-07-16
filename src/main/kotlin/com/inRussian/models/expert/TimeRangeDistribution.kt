package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
data class TimeRangeDistribution(
    val rangeLabel: String,
    val studentCount: Int,
    val percentage: Double
)