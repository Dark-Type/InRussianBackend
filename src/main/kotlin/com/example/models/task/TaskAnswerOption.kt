package com.example.models.task

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskAnswerOption(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val optionText: String? = null,
    val optionAudioId: String? = null,
    val isCorrect: Boolean = false,
    val orderNum: Int
)