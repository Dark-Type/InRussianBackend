package com.inRussian.responses.content

import com.inRussian.models.tasks.Task
import com.inRussian.models.tasks.TaskAnswer
import com.inRussian.models.tasks.TaskAnswerOption
import com.inRussian.models.tasks.TaskContent
import kotlinx.serialization.Serializable

@Serializable
data class FullTaskResponse(
    val task: Task,
    val content: List<TaskContent>,
    val answerOptions: List<TaskAnswerOption>,
    val answer: TaskAnswer? = null
)