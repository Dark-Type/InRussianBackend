package com.example.models.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskAnswerOptionRequest(
    val taskId: String,
    val optionText: String? = null,
    val optionAudioId: String? = null,
    val isCorrect: Boolean = false,
    val orderNum: Int
)