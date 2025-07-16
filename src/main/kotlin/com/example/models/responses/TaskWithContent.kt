package com.example.models.responses

import com.example.models.task.Task
import com.example.models.task.TaskAnswer
import com.example.models.task.TaskAnswerOption
import com.example.models.task.TaskContent
import kotlinx.serialization.Serializable

@Serializable
data class TaskWithContent(
    val task: Task,
    val content: List<TaskContent>,
    val answerOptions: List<TaskAnswerOption>,
    val answerConfig: TaskAnswer
)