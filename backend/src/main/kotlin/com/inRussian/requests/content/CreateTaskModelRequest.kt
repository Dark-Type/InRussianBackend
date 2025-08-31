package com.inRussian.requests.content

import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskModelRequest(
    val themeId: String,
    val taskBody: TaskBody,
    val taskTypes: List<TaskType>
)