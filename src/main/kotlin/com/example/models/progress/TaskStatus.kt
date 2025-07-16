package com.example.models.progress

import kotlinx.serialization.Serializable

@Serializable
enum class TaskStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, PENDING_RETRY
}