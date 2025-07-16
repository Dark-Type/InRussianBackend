package com.example.models.task

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val themeId: String,
    val name: String,
    val taskType: TaskType,
    val question: String,
    val instructions: String? = null,
    val isTraining: Boolean = false,
    val orderNum: Int,
    val createdAt: String = LocalDateTime.now().toString()
)


