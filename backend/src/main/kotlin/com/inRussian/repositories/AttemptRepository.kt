package com.inRussian.repositories

import com.inRussian.config.DatabaseFactory
import com.inRussian.models.v2.AttemptInsert
import com.inRussian.models.v2.AttemptRecord
import com.inRussian.tables.v2.UserTaskAttemptTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

interface AttemptRepository {
    suspend fun insertAttemptIfNew(a: AttemptInsert): Boolean
    suspend fun findAttemptById(attemptId: UUID): AttemptRecord?
    suspend fun getUserAttemptsByTheme(userId: UUID, themeId: UUID): List<AttemptRecord>
}

class ExposedAttemptRepository : com.inRussian.repositories.AttemptRepository {

    override suspend fun insertAttemptIfNew(a: AttemptInsert): Boolean = DatabaseFactory.dbQuery {
        val inserted = UserTaskAttemptTable.insertIgnore {
            it[id] = a.attemptId
            it[userId] = a.userId
            it[taskId] = a.taskId
            it[themeId] = a.themeId
            it[courseId] = a.courseId
            it[attemptsCount] = a.attemptsCount
            it[timeSpentMs] = a.timeSpentMs
            if (a.createdAt != null) {
                it[createdAt] = Instant.now().atZone(ZoneId.systemDefault()).toInstant()
            }
        }.insertedCount
        inserted > 0
    }

    override suspend fun findAttemptById(attemptId: UUID): AttemptRecord? = DatabaseFactory.dbQuery {
        UserTaskAttemptTable
            .selectAll()
            .where { UserTaskAttemptTable.id eq attemptId }
            .limit(1)
            .firstOrNull()
            ?.let(::toRecord)
    }

    override suspend fun getUserAttemptsByTheme(userId: UUID, themeId: UUID): List<AttemptRecord> =
        DatabaseFactory.dbQuery {
            UserTaskAttemptTable
                .selectAll()
                .where {
                    (UserTaskAttemptTable.userId eq userId) and
                            (UserTaskAttemptTable.themeId eq themeId)
                }
                .orderBy(UserTaskAttemptTable.createdAt, SortOrder.DESC)
                .map(::toRecord)
        }

    private fun toRecord(row: ResultRow): AttemptRecord =
        AttemptRecord(
            id = row[UserTaskAttemptTable.id].value,
            userId = row[UserTaskAttemptTable.userId],
            taskId = row[UserTaskAttemptTable.taskId],
            attemptsCount = row[UserTaskAttemptTable.attemptsCount],
            timeSpentMs = row[UserTaskAttemptTable.timeSpentMs],
            createdAt = row[UserTaskAttemptTable.createdAt].atOffset(ZoneOffset.UTC).toInstant()
        )
}