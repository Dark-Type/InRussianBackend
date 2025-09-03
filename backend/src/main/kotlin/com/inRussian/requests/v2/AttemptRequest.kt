package com.inRussian.requests.v2
import com.inRussian.models.v2.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AttemptRequest(
    @Serializable(with = UUIDSerializer::class) val attemptId: UUID,
    @Serializable(with = UUIDSerializer::class) val taskId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long
)