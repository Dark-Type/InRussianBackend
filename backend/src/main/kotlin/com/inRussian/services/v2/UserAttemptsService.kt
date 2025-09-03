package com.inRussian.services.v2

import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.v2.UserAttemptDTO
import com.inRussian.repositories.TaskRepository
import com.inRussian.repositories.v2.AttemptRepository
import com.inRussian.tables.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserAttemptService(
    private val attemptRepo: AttemptRepository
) {

    private val jsonConfig = Json {
        serializersModule = SerializersModule {
            polymorphic(TaskBody::class) {
                subclass(TaskBody.AudioConnectTask::class)
                subclass(TaskBody.TextConnectTask::class)
                subclass(TaskBody.TextInputTask::class)
                subclass(TaskBody.TextInputWithVariantTask::class)
                subclass(TaskBody.ImageConnectTask::class)
                subclass(TaskBody.ListenAndSelect::class)
                subclass(TaskBody.ImageAndSelect::class)
                subclass(TaskBody.ConstructSentenceTask::class)
                subclass(TaskBody.SelectWordsTask::class)
            }
            contextual(ByteArray::class) {
                TaskRepository.ByteArraySerializer
            }
        }
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun getUserSectionAttempts(userId: UUID, sectionId: UUID): List<UserAttemptDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            val attempts = attemptRepo.getUserAttemptsBySection(userId, sectionId)

            attempts.map { attempt ->
                val task = TaskEntity
                    .selectAll()
                    .where { TaskEntity.id eq attempt.taskId }
                    .first()

                val taskBody = jsonConfig.decodeFromJsonElement(
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