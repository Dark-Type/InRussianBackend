package com.inRussian.responses.statistics

import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class AverageTaskTimeResponse(
    val taskId: String,
    val taskName: String,
    val taskType: TaskType,
    val averageTimeSeconds: Double,
    val totalAttempts: Int
)