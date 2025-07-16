package com.example.models.responses.statistics

import com.example.models.task.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class AverageTaskTimeResponse(
    val taskId: String,
    val taskName: String,
    val taskType: TaskType,
    val averageTimeSeconds: Double,
    val totalAttempts: Int
)