package com.inRussian.requests.v2
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.util.UUID

@Serializable
data class AttemptRequest(
    @Contextual val attemptId: UUID,
    @Contextual val taskId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long
)