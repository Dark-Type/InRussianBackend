package com.inRussian.repositories.v2


import com.inRussian.models.v2.AttemptInsert
import com.inRussian.models.v2.AttemptRecord
import com.inRussian.tables.v2.UserTaskAttemptTable
import kotlinx.coroutines.Dispatchers

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertIgnore

import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.compareTo
import kotlin.text.get
import kotlin.text.set


class AttemptRepository {

    suspend fun insertAttemptIfNew(a: AttemptInsert): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val inserted = UserTaskAttemptTable.insertIgnore {
            it[id] = a.attemptId
            it[userId] = a.userId
            it[taskId] = a.taskId
            it[themeId] = a.themeId
            it[sectionId] = a.sectionId
            it[courseId] = a.courseId
            it[attemptsCount] = a.attemptsCount
            it[timeSpentMs] = a.timeSpentMs
            if (a.createdAt != null) {
                it[createdAt] = Instant.now().atZone(ZoneId.systemDefault()).toInstant()
            }
        }.insertedCount
        inserted > 0
    }

    suspend fun findAttemptById(attemptId: UUID): AttemptRecord? = newSuspendedTransaction(Dispatchers.IO) {
        UserTaskAttemptTable
            .selectAll()
            .where { UserTaskAttemptTable.id eq attemptId }
            .limit(1)
            .firstOrNull()
            ?.let(::toRecord)
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