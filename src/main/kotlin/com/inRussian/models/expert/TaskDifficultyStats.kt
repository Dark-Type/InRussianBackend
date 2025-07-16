package com.inRussian.models.expert

import com.inRussian.models.tasks.TaskType
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