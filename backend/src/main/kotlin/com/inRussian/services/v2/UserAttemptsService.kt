package com.inRussian.services.v2

import com.inRussian.config.appJson
import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.v2.UserAttemptDTO
import com.inRussian.repositories.AttemptRepository
import com.inRussian.tables.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.PolymorphicSerializer
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserAttemptService(
    private val attemptRepo: AttemptRepository
) {


    suspend fun getUserThemeAttempts(userId: UUID, themeId: UUID): List<UserAttemptDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            val attempts = attemptRepo.getUserAttemptsByTheme(userId, themeId)

            attempts.map { attempt ->
                val task = TaskEntity
                    .selectAll()
                    .where { TaskEntity.id eq attempt.taskId }
                    .first()

                val taskBody = appJson.decodeFromJsonElement(
                    PolymorphicSerializer(TaskBody::class),
                    task[TaskEntity.taskBody]
                )

                UserAttemptDTO(
                    attemptId = attempt.id,
                    taskId = attempt.taskId,
                    taskQuestion = task[TaskEntity.question],
                    taskBody = taskBody,
                    attemptsCount = attempt.attemptsCount,
                    timeSpentMs = attempt.timeSpentMs,
                    createdAt = attempt.createdAt
                )
            }
        }
}