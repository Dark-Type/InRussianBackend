package com.inRussian.models.courses

import com.inRussian.models.tasks.Task
import com.inRussian.models.tasks.TaskAnswerItem
import com.inRussian.models.tasks.TaskAnswerOptionItem
import com.inRussian.models.tasks.TaskContentItem
import kotlinx.serialization.Serializable

@Serializable
data class TaskWithContent(
    val task: Task,
    val content: List<TaskContentItem>,
    val answerOptions: List<TaskAnswerOptionItem>,
    val answerConfig: TaskAnswerItem
)