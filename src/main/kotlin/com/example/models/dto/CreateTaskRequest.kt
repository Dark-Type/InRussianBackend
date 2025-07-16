package com.example.models.dto

import com.example.models.task.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskRequest(
    val themeId: String,
    val name: String,
    val taskType: TaskType,
    val question: String,
    val instructions: String? = null,
    val isTraining: Boolean = false,
    val orderNum: Int
)