package com.example.models.expertRelated

import kotlinx.serialization.Serializable

@Serializable
data class TimeRangeDistribution(
    val rangeLabel: String,
    val studentCount: Int,
    val percentage: Double
)