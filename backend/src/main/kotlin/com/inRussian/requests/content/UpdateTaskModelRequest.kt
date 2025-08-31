package com.inRussian.requests.content

import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateTaskModelRequest(
    val themeId: String? = null,
    val taskBody: TaskBody? = null,
    val taskTypes: List<TaskType>? = null,
    val question: String? = null
)