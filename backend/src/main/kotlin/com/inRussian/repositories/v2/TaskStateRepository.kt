package com.inRussian.repositories.v2

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.tables.v2.UserTaskStateTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID


class TaskStateRepository {

    suspend fun ensureStateRow(
        userId: UUID,
        taskId: UUID,
        themeId: UUID,
        courseId: UUID
    ) = dbQuery {
        UserTaskStateTable.insertIgnore {
            it[UserTaskStateTable.userId] = userId
            it[UserTaskStateTable.taskId] = taskId
            it[UserTaskStateTable.themeId] = themeId
            it[UserTaskStateTable.courseId] = courseId
        }
    }

    suspend fun markSolvedFirstTryIfNot(
        userId: UUID,
        taskId: UUID,
        solvedAt: Instant = Instant.now()
    ): Boolean = dbQuery {
        val updated = UserTaskStateTable.update(
            where = {
                (UserTaskStateTable.userId eq userId) and
                        (UserTaskStateTable.taskId eq taskId) and
                        (UserTaskStateTable.isSolvedFirstTry eq false)
            }
        ) {
            it[UserTaskStateTable.isSolvedFirstTry] = true
            it[UserTaskStateTable.firstSolvedAt] = Instant.now()
        }
        updated > 0
    }

    suspend fun isSolved(userId: UUID, taskId: UUID): Boolean = dbQuery {
        UserTaskStateTable
            .selectAll()
            .where { (UserTaskStateTable.userId eq userId) and (UserTaskStateTable.taskId eq taskId) }
            .limit(1)
            .firstOrNull()
            ?.get(UserTaskStateTable.isSolvedFirstTry)
            ?: false
    }
}