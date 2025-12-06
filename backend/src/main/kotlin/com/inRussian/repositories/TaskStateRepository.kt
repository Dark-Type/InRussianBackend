package com.inRussian.repositories

import com.inRussian.config.DatabaseFactory
import com.inRussian.config.dbQuery
import com.inRussian.tables.v2.UserTaskStateTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

interface TaskStateRepository {
    suspend fun ensureStateRow(
        userId: UUID,
        taskId: UUID,
        themeId: UUID,
        courseId: UUID
    ): Unit

    suspend fun markSolvedFirstTryIfNot(
        userId: UUID,
        taskId: UUID,
        solvedAt: Instant = Instant.now()
    ): Boolean

    suspend fun isSolved(userId: UUID, taskId: UUID): Boolean
}
class ExposedTaskStateRepository : TaskStateRepository {

    override suspend fun ensureStateRow(
        userId: UUID,
        taskId: UUID,
        themeId: UUID,
        courseId: UUID
    ) {
        dbQuery {
            UserTaskStateTable.insertIgnore {
                it[UserTaskStateTable.userId] = userId
                it[UserTaskStateTable.taskId] = taskId
                it[UserTaskStateTable.themeId] = themeId
                it[UserTaskStateTable.courseId] = courseId
            }
        }
    }

    override suspend fun markSolvedFirstTryIfNot(
        userId: UUID,
        taskId: UUID,
        solvedAt: Instant
    ): Boolean = dbQuery {
        UserTaskStateTable.update({
            (UserTaskStateTable.userId eq userId) and
                    (UserTaskStateTable.taskId eq taskId) and
                    (UserTaskStateTable.isSolvedFirstTry eq false)
        }) {
            it[UserTaskStateTable.isSolvedFirstTry] = true
            it[UserTaskStateTable.firstSolvedAt] = solvedAt
        } > 0
    }

    override suspend fun isSolved(userId: UUID, taskId: UUID): Boolean = dbQuery {
        UserTaskStateTable.selectAll()
            .where { (UserTaskStateTable.userId eq userId) and (UserTaskStateTable.taskId eq taskId) }
            .limit(1)
            .firstOrNull()
            ?.get(UserTaskStateTable.isSolvedFirstTry)
            ?: false
    }
}