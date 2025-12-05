package com.inRussian.repositories.v2

import com.inRussian.config.dbQuery
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.v2.UserThemeQueueItemTable
import com.inRussian.tables.v2.UserThemeQueueStateTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

import java.util.UUID


class QueueRepository {

    suspend fun ensureQueueState(userId: UUID, themeId: UUID) = dbQuery {
        UserThemeQueueStateTable.insertIgnore {
            it[UserThemeQueueStateTable.userId] = userId
            it[UserThemeQueueStateTable.themeId] = themeId
        }
    }

    suspend fun getQueueSize(userId: UUID, themeId: UUID): Long = dbQuery {
        UserThemeQueueItemTable
            .selectAll()
            .where { (UserThemeQueueItemTable.userId eq userId) and (UserThemeQueueItemTable.themeId eq themeId) }
            .count()
    }

    suspend fun nextItemTaskId(userId: UUID, themeId: UUID): UUID? = dbQuery {
        UserThemeQueueItemTable
            .selectAll()
            .where { (UserThemeQueueItemTable.userId eq userId) and (UserThemeQueueItemTable.themeId eq themeId) }
            .orderBy(UserThemeQueueItemTable.position to SortOrder.ASC)
            .limit(1)
            .firstOrNull()
            ?.get(UserThemeQueueItemTable.taskId)
    }

    suspend fun removeFromQueue(userId: UUID, themeId: UUID, taskId: UUID): Int = dbQuery {
        UserThemeQueueItemTable.deleteWhere {
            (UserThemeQueueItemTable.userId eq userId) and
                    (UserThemeQueueItemTable.themeId eq themeId) and
                    (UserThemeQueueItemTable.taskId eq taskId)
        }
    }

    suspend fun moveToEnd(userId: UUID, themeId: UUID, taskId: UUID): Int = dbQuery {
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
        updated
    }

    suspend fun enqueueTasksAtEnd(userId: UUID, themeId: UUID, taskIds: List<UUID>): Int = dbQuery {
        if (taskIds.isEmpty()) return@dbQuery 0

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
        inserted
    }

    suspend fun seedThemeTasks(userId: UUID, themeId: UUID): Int = dbQuery {
        val taskIds = TaskEntity
            .selectAll()
            .where { TaskEntity.themeId eq themeId }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)
            .map { it[TaskEntity.id].value }

        if (taskIds.isEmpty()) return@dbQuery 0

        // Call enqueueTasksAtEnd within the same transaction context
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
        inserted
    }
}