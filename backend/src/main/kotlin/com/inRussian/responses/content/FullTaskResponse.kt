package com.inRussian.responses.content

import com.inRussian.models.tasks.Task
import com.inRussian.models.tasks.TaskAnswerItem
import com.inRussian.models.tasks.TaskAnswerOptionItem
import com.inRussian.models.tasks.TaskContentItem
import kotlinx.serialization.Serializable

@Serializable
data class FullTaskResponse(
    val task: Task,
    val content: List<TaskContentItem>,
    val answerOptions: List<TaskAnswerOptionItem>,
    val answer: TaskAnswerItem? = null
)