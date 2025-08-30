package com.inRussian.requests.contentManager

import com.inRussian.models.tasks.TaskType
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