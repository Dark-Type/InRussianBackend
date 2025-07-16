package com.example.models.contentManager

import com.example.models.task.ContentType
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