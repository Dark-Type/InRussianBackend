package com.inRussian.models.tasks

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskAnswerOptionItem(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val optionText: String? = null,
    val optionAudioId: String? = null,
    val isCorrect: Boolean = false,
    val orderNum: Int
)