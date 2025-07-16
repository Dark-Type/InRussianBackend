package com.inRussian.models.progress

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UserTaskQueue(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val taskId: String,
    val themeId: String,
    val queuePosition: Int,
    val isOriginalTask: Boolean = true,
    val isRetryTask: Boolean = false,
    val originalTaskId: String? = null, // Отсылает к основному заданию, если это повтор
    val createdAt: String = LocalDateTime.now().toString()
)