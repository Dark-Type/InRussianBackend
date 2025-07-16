package com.example.models.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class PeriodStatisticsRequest(
    val dateFrom: String,
    val dateTo: String,
    val courseId: String? = null,
    val userId: String? = null
)