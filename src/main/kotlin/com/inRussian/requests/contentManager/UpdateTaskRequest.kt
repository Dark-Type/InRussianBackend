package com.inRussian.requests.contentManager

import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateTaskRequest(
    val name: String? = null,
    val taskType: TaskType? = null,
    val question: String? = null,
    val instructions: String? = null,
    val isTraining: Boolean? = null,
    val orderNum: Int? = null
)