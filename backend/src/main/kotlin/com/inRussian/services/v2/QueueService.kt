package com.inRussian.services.v2

import com.inRussian.models.v2.NextTaskResult
import com.inRussian.repositories.v2.QueueRepository
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserSectionQueueItemTable
import com.inRussian.tables.v2.UserSectionQueueStateTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.text.get

class QueueService(
    private val queueRepo: QueueRepository
) {

    suspend fun ensureQueueState(userId: UUID, sectionId: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        queueRepo.ensureQueueState(userId, sectionId)
    }

    suspend fun getOrSeedNextTask(userId: UUID, sectionId: UUID): NextTaskResult? =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.ensureQueueState(userId, sectionId)

            var size = queueRepo.getQueueSize(userId, sectionId)
            if (size == 0L) {
                queueRepo.seedNextTheme(userId, sectionId)
                size = queueRepo.getQueueSize(userId, sectionId)
            } else if (size == 1L) {
                queueRepo.seedNextTheme(userId, sectionId)
            }

            val nextTaskId = queueRepo.nextItemTaskId(userId, sectionId) ?: return@newSuspendedTransaction null
            val themeId = TaskEntity
                .selectAll().where { TaskEntity.id eq nextTaskId }
                .limit(1)
                .first()[TaskEntity.themeId].value

            NextTaskResult(
                taskId = nextTaskId,
                sectionId = sectionId,
                themeId = themeId
            )
        }

    suspend fun removeFromQueue(userId: UUID, sectionId: UUID, taskId: UUID): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.removeFromQueue(userId, sectionId, taskId) > 0
        }

    suspend fun moveToEnd(userId: UUID, sectionId: UUID, taskId: UUID): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.moveToEnd(userId, sectionId, taskId) > 0
        }

    suspend fun maybeSeedLowWatermark(userId: UUID, sectionId: UUID) =
        newSuspendedTransaction(Dispatchers.IO) {
            val size = queueRepo.getQueueSize(userId, sectionId)
            if (size == 1L) {
                queueRepo.seedNextTheme(userId, sectionId)
            } else if (size == 0L) {
                queueRepo.seedNextTheme(userId, sectionId)
            }
        }

    suspend fun currentQueueSize(userId: UUID, sectionId: UUID): Long =
        newSuspendedTransaction(Dispatchers.IO) {
            queueRepo.getQueueSize(userId, sectionId)
        }
}