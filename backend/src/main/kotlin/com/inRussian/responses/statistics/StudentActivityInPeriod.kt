package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StudentActivityInPeriod(
    val userId: String,
    val userName: String,
    val tasksCompletedInPeriod: Int,
    val timeSpentInPeriod: Int,
    val progressIncrease: Double
)