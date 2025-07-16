package com.inRussian.models.courses

import com.inRussian.models.tasks.Task
import com.inRussian.models.tasks.TaskAnswer
import com.inRussian.models.tasks.TaskAnswerOption
import com.inRussian.models.tasks.TaskContent
import kotlinx.serialization.Serializable

@Serializable
data class TaskWithContent(
    val task: Task,
    val content: List<TaskContent>,
    val answerOptions: List<TaskAnswerOption>,
    val answerConfig: TaskAnswer
)