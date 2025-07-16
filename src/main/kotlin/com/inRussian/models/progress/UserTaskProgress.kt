package com.inRussian.models.progress

import kotlinx.serialization.Serializable

@Serializable
data class UserTaskProgress(
    val userId: String,
    val taskId: String,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val attemptCount: Int = 0,
    val isCorrect: Boolean? = null,
    val lastAttemptAt: String? = null,
    val completedAt: String? = null,
    val shouldRetryAfterTasks: Int? = null
)