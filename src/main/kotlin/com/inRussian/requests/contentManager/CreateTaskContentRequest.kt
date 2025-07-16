package com.inRussian.requests.contentManager

import com.inRussian.models.tasks.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class CreateTaskContentRequest(
    val taskId: String,
    val contentType: ContentType,
    val contentId: String? = null,
    val description: String? = null,
    val transcription: String? = null,
    val translation: String? = null,
    val orderNum: Int
)