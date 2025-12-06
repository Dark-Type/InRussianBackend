package com.inRussian.services.v2

import com.inRussian.models.v2.NextTaskResult
import com.inRussian.repositories.QueueRepository
import java.util.UUID

class QueueService(
    private val queueRepo: QueueRepository
) {

    suspend fun ensureQueueState(userId: UUID, themeId: UUID) {
        queueRepo.ensureQueueState(userId, themeId)
    }

    suspend fun getOrSeedNextTask(userId: UUID, themeId: UUID): NextTaskResult? {
        queueRepo.ensureQueueState(userId, themeId)

        var size = queueRepo.getQueueSize(userId, themeId)
        if (size == 0L) {
            queueRepo.seedThemeTasks(userId, themeId)
            size = queueRepo.getQueueSize(userId, themeId)
        } else if (size == 1L) {
            queueRepo.seedThemeTasks(userId, themeId)
        }

        val nextTaskId = queueRepo.nextItemTaskId(userId, themeId) ?: return null

        return NextTaskResult(
            taskId = nextTaskId,
            themeId = themeId
        )
    }

    suspend fun removeFromQueue(userId: UUID, themeId: UUID, taskId: UUID): Boolean =
        queueRepo.removeFromQueue(userId, themeId, taskId) > 0

    suspend fun moveToEnd(userId: UUID, themeId: UUID, taskId: UUID): Boolean =
        queueRepo.moveToEnd(userId, themeId, taskId) > 0

    suspend fun maybeSeedLowWatermark(userId: UUID, themeId: UUID) {
        val size = queueRepo.getQueueSize(userId, themeId)
        if (size == 1L || size == 0L) {
            queueRepo.seedThemeTasks(userId, themeId)
        }
    }

    suspend fun currentQueueSize(userId: UUID, themeId: UUID): Long =
        queueRepo.getQueueSize(userId, themeId)
}