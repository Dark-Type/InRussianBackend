package com.inRussian.services.v2

import com.inRussian.models.v2.NextTaskResult
import com.inRussian.repositories.v2.QueueRepository
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class QueueService(
    private val queueRepo: QueueRepository
) {

    suspend fun ensureQueueState(userId: UUID, themeId: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        queueRepo.ensureQueueState(userId, themeId)
    }

    suspend fun getOrSeedNextTask(userId: UUID, themeId: UUID): NextTaskResult? =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.ensureQueueState(userId, themeId)

            var size = queueRepo.getQueueSize(userId, themeId)
            if (size == 0L) {
                queueRepo.seedThemeTasks(userId, themeId)
                size = queueRepo.getQueueSize(userId, themeId)
            } else if (size == 1L) {
                queueRepo.seedThemeTasks(userId, themeId)
            }

            val nextTaskId = queueRepo.nextItemTaskId(userId, themeId) ?: return@newSuspendedTransaction null

            NextTaskResult(
                taskId = nextTaskId,
                themeId = themeId
            )
        }

    suspend fun removeFromQueue(userId: UUID, themeId: UUID, taskId: UUID): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.removeFromQueue(userId, themeId, taskId) > 0
        }

    suspend fun moveToEnd(userId: UUID, themeId: UUID, taskId: UUID): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.moveToEnd(userId, themeId, taskId) > 0
        }

    suspend fun maybeSeedLowWatermark(userId: UUID, themeId: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            val size = queueRepo.getQueueSize(userId, themeId)
            if (size == 1L || size == 0L) {
                queueRepo.seedThemeTasks(userId, themeId)
            }
        }

    suspend fun currentQueueSize(userId: UUID, themeId: UUID): Long =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.getQueueSize(userId, themeId)
        }
}