package com.inRussian.models.tasks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.*

@Serializable
data class TaskAnswer(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val answerType: AnswerType,
    val correctAnswer: JsonElement
)