package com.inRussian.requests.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class UpdateTaskAnswerOptionRequest(
    val optionText: String? = null,
    val optionAudioId: String? = null,
    val isCorrect: Boolean? = null,
    val orderNum: Int? = null
)