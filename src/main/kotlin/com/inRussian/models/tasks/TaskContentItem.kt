package com.inRussian.models.tasks

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class TaskContentItem(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val contentType: ContentType,
    val contentId: String? = null,
    val description: String? = null,
    val transcription: String? = null,
    val translation: String? = null,
    val orderNum: Int
)