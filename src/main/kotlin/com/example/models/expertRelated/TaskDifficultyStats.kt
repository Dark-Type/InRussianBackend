package com.example.models.expertRelated

import com.example.models.task.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class TaskDifficultyStats(
    val taskId: String,
    val taskName: String,
    val taskType: TaskType,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val successRate: Double,
    val averageTimeSeconds: Double,
    val difficultyLevel: TaskDifficultyLevel
)