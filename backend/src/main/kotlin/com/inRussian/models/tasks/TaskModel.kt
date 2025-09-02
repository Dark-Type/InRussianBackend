package com.inRussian.models.tasks

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class TaskModel @OptIn(ExperimentalTime::class) constructor(
    val id: String = UUID.randomUUID().toString(),
    val taskType: List<TaskType>,

    val taskBody: TaskBody,
    val question: String?,
    val createdAt: String = Clock.System.now().toString(),
    val updatedAt: String = Clock.System.now().toString(),
)