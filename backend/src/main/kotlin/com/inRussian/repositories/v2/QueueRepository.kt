package com.inRussian.repositories.v2

import com.inRussian.tables.TaskEntity
import com.inRussian.tables.v2.UserThemeQueueItemTable
import com.inRussian.tables.v2.UserThemeQueueStateTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

import java.util.UUID


class QueueRepository {

    fun ensureQueueState(userId: UUID, themeId: UUID) {
        UserThemeQueueStateTable.insertIgnore {
            it[UserThemeQueueStateTable.userId] = userId
            it[UserThemeQueueStateTable.themeId] = themeId
        }
    }

    fun getQueueSize(userId: UUID, themeId: UUID): Long =
        UserThemeQueueItemTable
            .selectAll()
            .where { (UserThemeQueueItemTable.userId eq userId) and (UserThemeQueueItemTable.themeId eq themeId) }
            .count()

    fun nextItemTaskId(userId: UUID, themeId: UUID): UUID? =
        UserThemeQueueItemTable
            .selectAll()
            .where { (UserThemeQueueItemTable.userId eq userId) and (UserThemeQueueItemTable.themeId eq themeId) }
            .orderBy(UserThemeQueueItemTable.position to SortOrder.ASC)
            .limit(1)
            .firstOrNull()
            ?.get(UserThemeQueueItemTable.taskId)

    fun removeFromQueue(userId: UUID, themeId: UUID, taskId: UUID): Int =
        UserThemeQueueItemTable.deleteWhere {
            (UserThemeQueueItemTable.userId eq userId) and
                    (UserThemeQueueItemTable.themeId eq themeId) and
                    (UserThemeQueueItemTable.taskId eq taskId)
        }

    fun moveToEnd(userId: UUID, themeId: UUID, taskId: UUID): Int {
        val state = UserThemeQueueStateTable
            .selectAll()
            .where { (UserThemeQueueStateTable.userId eq userId) and (UserThemeQueueStateTable.themeId eq themeId) }
            .firstOrNull() ?: throw IllegalStateException("Queue state missing for user=$userId theme=$themeId")

        val newPos = state[UserThemeQueueStateTable.lastPosition] + 1
        val updated = UserThemeQueueItemTable.update(
            where = {
                (UserThemeQueueItemTable.userId eq userId) and
                        (UserThemeQueueItemTable.themeId eq themeId) and
                        (UserThemeQueueItemTable.taskId eq taskId)
            }
        ) {
            it[position] = newPos
        }

        UserThemeQueueStateTable.update(
            where = { (UserThemeQueueStateTable.userId eq userId) and (UserThemeQueueStateTable.themeId eq themeId) }
        ) {
            it[lastPosition] = newPos
        }
        return updated
    }

    fun enqueueTasksAtEnd(userId: UUID, themeId: UUID, taskIds: List<UUID>): Int {
        if (taskIds.isEmpty()) return 0
        val state = UserThemeQueueStateTable
            .selectAll()
            .where { (UserThemeQueueStateTable.userId eq userId) and (UserThemeQueueStateTable.themeId eq themeId) }
            .firstOrNull() ?: throw IllegalStateException("Queue state missing")

        var pos = state[UserThemeQueueStateTable.lastPosition]
        var inserted = 0
        taskIds.forEach { taskId ->
            pos += 1
            inserted += UserThemeQueueItemTable.insertIgnore {
                it[UserThemeQueueItemTable.userId] = userId
                it[UserThemeQueueItemTable.themeId] = themeId
                it[UserThemeQueueItemTable.taskId] = taskId
                it[UserThemeQueueItemTable.position] = pos
            }.insertedCount
        }
        if (inserted > 0) {
            UserThemeQueueStateTable.update(
                where = { (UserThemeQueueStateTable.userId eq userId) and (UserThemeQueueStateTable.themeId eq themeId) }
            ) { it[lastPosition] = pos }
        }
        return inserted
    }

    fun seedThemeTasks(userId: UUID, themeId: UUID): Int {
        val taskIds = TaskEntity
            .selectAll()
            .where { TaskEntity.themeId eq themeId }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)
            .map { it[TaskEntity.id].value }

        return enqueueTasksAtEnd(userId, themeId, taskIds)
    }
}