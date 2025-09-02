package com.inRussian.repositories.v2

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
        sectionId: UUID,
        courseId: UUID
    ) = newSuspendedTransaction(Dispatchers.IO) {
        UserTaskStateTable.insertIgnore {
            it[UserTaskStateTable.userId] = userId
            it[UserTaskStateTable.taskId] = taskId
            it[UserTaskStateTable.themeId] = themeId
            it[UserTaskStateTable.sectionId] = sectionId
            it[UserTaskStateTable.courseId] = courseId
        }
    }

    suspend fun markSolvedFirstTryIfNot(
        userId: UUID,
        taskId: UUID,
        solvedAt: Instant = Instant.now()
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
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

    suspend fun isSolved(userId: UUID, taskId: UUID): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        UserTaskStateTable
            .selectAll()
            .where { (UserTaskStateTable.userId eq userId) and (UserTaskStateTable.taskId eq taskId) }
            .limit(1)
            .firstOrNull()
            ?.get(UserTaskStateTable.isSolvedFirstTry)
            ?: false
    }
}