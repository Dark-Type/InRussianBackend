package com.inRussian.repositories.v2

import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserSectionQueueItemTable
import com.inRussian.tables.v2.UserSectionQueueStateTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.and
import kotlin.text.get
import kotlin.text.set
import kotlin.text.toLong

class QueueRepository {

    fun ensureQueueState(userId: UUID, sectionId: UUID) {
        UserSectionQueueStateTable.insertIgnore {
            it[UserSectionQueueStateTable.userId] = userId
            it[UserSectionQueueStateTable.sectionId] = sectionId
        }
    }

    fun getQueueSize(userId: UUID, sectionId: UUID): Long =
        UserSectionQueueItemTable
            .selectAll().where { (UserSectionQueueItemTable.userId eq userId) and (UserSectionQueueItemTable.sectionId eq sectionId) }
            .count()

    fun nextItemTaskId(userId: UUID, sectionId: UUID): UUID? =
        UserSectionQueueItemTable
            .selectAll().where { (UserSectionQueueItemTable.userId eq userId) and (UserSectionQueueItemTable.sectionId eq sectionId) }
            .orderBy(UserSectionQueueItemTable.position to SortOrder.ASC)
            .limit(1)
            .firstOrNull()
            ?.get(UserSectionQueueItemTable.taskId)

    fun removeFromQueue(userId: UUID, sectionId: UUID, taskId: UUID): Int =
        UserSectionQueueItemTable.deleteWhere {
            (UserSectionQueueItemTable.userId eq userId) and
                    (UserSectionQueueItemTable.sectionId eq sectionId) and
                    (UserSectionQueueItemTable.taskId eq taskId)
        }

    fun moveToEnd(userId: UUID, sectionId: UUID, taskId: UUID): Int {
        val state = UserSectionQueueStateTable
            .selectAll().where { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
            .firstOrNull() ?: throw IllegalStateException("Queue state missing for user=$userId section=$sectionId")

        val newPos = state[UserSectionQueueStateTable.lastPosition] + 1
        val updated = UserSectionQueueItemTable.update(
            where = {
                (UserSectionQueueItemTable.userId eq userId) and
                        (UserSectionQueueItemTable.sectionId eq sectionId) and
                        (UserSectionQueueItemTable.taskId eq taskId)
            }
        ) {
            it[position] = newPos
        }

        UserSectionQueueStateTable.update(
            where = { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
        ) {
            it[lastPosition] = newPos
        }
        return updated
    }

    fun enqueueTasksAtEnd(userId: UUID, sectionId: UUID, taskIds: List<UUID>): Int {
        if (taskIds.isEmpty()) return 0
        val state = UserSectionQueueStateTable
            .selectAll().where { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
            .firstOrNull() ?: throw IllegalStateException("Queue state missing")

        var pos = state[UserSectionQueueStateTable.lastPosition]
        var inserted = 0
        taskIds.forEach { taskId ->
            pos += 1
            inserted += UserSectionQueueItemTable.insertIgnore {
                it[UserSectionQueueItemTable.userId] = userId
                it[UserSectionQueueItemTable.sectionId] = sectionId
                it[UserSectionQueueItemTable.taskId] = taskId
                it[UserSectionQueueItemTable.position] = pos
            }.insertedCount
        }
        if (inserted > 0) {
            UserSectionQueueStateTable.update(
                where = { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
            ) { it[lastPosition] = pos }
        }
        return inserted
    }

    fun seedNextTheme(userId: UUID, sectionId: UUID): Int {
        val stateRow = UserSectionQueueStateTable
            .selectAll().where { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
            .firstOrNull() ?: throw IllegalStateException("Queue state missing")

        val lastSeeded = stateRow[UserSectionQueueStateTable.lastSeededOrderNum]

        val nextThemeRow = Themes
            .selectAll().where { (Themes.sectionId eq sectionId) and (Themes.orderNum greater  (lastSeeded ?: Int.MIN_VALUE)) }
            .orderBy(Themes.orderNum to SortOrder.ASC)
            .firstOrNull() ?: return 0

        val nextThemeId = nextThemeRow[Themes.id].value
        val nextOrderNum = nextThemeRow[Themes.orderNum]

        val taskIds = TaskEntity
            .selectAll().where { TaskEntity.themeId eq nextThemeId }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)
            .map { it[TaskEntity.id].value }

        if (taskIds.isEmpty()) {
            UserSectionQueueStateTable.update(
                where = { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
            ) { it[lastSeededOrderNum] = nextOrderNum }
            return seedNextTheme(userId, sectionId)
        }

        val enq = enqueueTasksAtEnd(userId, sectionId, taskIds)

        UserSectionQueueStateTable.update(
            where = { (UserSectionQueueStateTable.userId eq userId) and (UserSectionQueueStateTable.sectionId eq sectionId) }
        ) { it[lastSeededOrderNum] = nextOrderNum }

        return enq
    }
}