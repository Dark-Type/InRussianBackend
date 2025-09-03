package com.inRussian.repositories

import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskModel
import com.inRussian.models.tasks.TaskType
import com.inRussian.requests.content.CreateTaskModelRequest
import com.inRussian.requests.content.UpdateTaskModelRequest
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.TaskToTypes
import com.inRussian.tables.TaskTypes
import com.inRussian.tables.json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class TaskRepository {
    object ByteArraySerializer : KSerializer<ByteArray> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

        @OptIn(ExperimentalEncodingApi::class)
        override fun serialize(encoder: Encoder, value: ByteArray) {
            encoder.encodeString(Base64.encode(value))
        }

        @OptIn(ExperimentalEncodingApi::class)
        override fun deserialize(decoder: Decoder): ByteArray {
            return Base64.decode(decoder.decodeString())
        }
    }

    fun updateTask(taskId: UUID, request: UpdateTaskModelRequest): TaskModel? = transaction {
        val updated = TaskEntity.update({ TaskEntity.id eq taskId }) { st ->
            request.themeId?.let { st[TaskEntity.themeId] = UUID.fromString(it) }
            request.taskBody?.let { st[TaskEntity.taskBody] = json.encodeToJsonElement(it) }
            request.question?.let { st[TaskEntity.question] = it }
            st[TaskEntity.updatedAt] = CurrentTimestamp
        }

        if (updated == 0) return@transaction null

        request.taskTypes?.let { newTypes ->
            TaskToTypes.deleteWhere { TaskToTypes.task eq taskId }

            newTypes.forEach { taskType ->
                val typeExists = TaskTypes
                    .selectAll()
                    .where { TaskTypes.name eq taskType.name }
                    .count() > 0
                if (!typeExists) {
                    TaskTypes.insert { it[name] = taskType.name }
                }
                TaskToTypes.insert {
                    it[task] = taskId
                    it[typeName] = taskType.name
                }
            }
        }

        selectTaskDtoById(taskId)
    }

    fun deleteTask(taskId: UUID): Boolean = transaction {
        TaskToTypes.deleteWhere { TaskToTypes.task eq taskId }
        val deleted = TaskEntity.deleteWhere { TaskEntity.id eq taskId }
        deleted > 0
    }

    fun createTask(request: CreateTaskModelRequest): TaskModel = transaction {
        addLogger(StdOutSqlLogger)

        val taskBodyJson: JsonElement = json.encodeToJsonElement(request.taskBody)

        val insertedId = TaskEntity.insertAndGetId { task ->
            task[themeId] = UUID.fromString(request.themeId)
            task[taskBody] = taskBodyJson
            task[question] = request.question
        }.value

        request.taskTypes.forEach { taskType ->
            val typeExists = TaskTypes
                .selectAll()
                .where { TaskTypes.name eq taskType.name }
                .count() > 0

            if (!typeExists) {
                TaskTypes.insert { it[name] = taskType.name }
            }

            TaskToTypes.insert {
                it[task] = insertedId
                it[typeName] = taskType.name
            }
        }

        selectTaskDtoById(insertedId)
            ?: throw IllegalStateException("Created task not found: $insertedId")
    }

    fun getTaskById(id: UUID): TaskModel = transaction {
        selectTaskDtoById(id)
            ?: throw IllegalStateException("Created task not found: $id")
    }

    fun getTaskByThemeId(id: UUID): List<TaskModel> = transaction {
        val tasks = TaskEntity.selectAll().where { TaskEntity.themeId eq id }.map { it[TaskEntity.id].value }

        tasks.map { taskId ->
            selectTaskDtoById(taskId)
                ?: throw IllegalStateException("Created task not found: $id")
        }
    }


    private fun selectTaskDtoById(taskId: UUID): TaskModel? {
        val taskEntity = TaskEntity
            .selectAll()
            .where { TaskEntity.id eq taskId }
            .singleOrNull() ?: return null


        val taskTypes =
            TaskToTypes.join(
                TaskTypes,
                JoinType.INNER,
                onColumn = TaskToTypes.typeName,
                otherColumn = TaskTypes.name
            )
                .selectAll()
                .where { TaskToTypes.task eq taskId }
                .map { row: ResultRow ->

                    val cleaned = row[TaskToTypes.typeName].trim().trim('"')
                    enumValues<TaskType>().first { it.name.equals(cleaned, ignoreCase = true) }
                }
        val taskBody = Json {
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
                    ByteArraySerializer
                }
            }
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.decodeFromJsonElement(
            PolymorphicSerializer(TaskBody::class),
            taskEntity[TaskEntity.taskBody]
        )
        return TaskModel(
            id = taskEntity[TaskEntity.id].value.toString(),
            taskType = taskTypes,
            taskBody = taskBody,
            question = taskEntity[TaskEntity.question],
            createdAt = taskEntity[TaskEntity.createdAt].toString(),
            updatedAt = taskEntity[TaskEntity.updatedAt].toString()
        )
    }

    private fun ResultRow.toTaskDto(types: List<TaskType>): TaskModel =
        TaskModel(
            id = this[TaskEntity.id].value.toString(),
            taskType = types,
            taskBody = json.decodeFromJsonElement(this[TaskEntity.taskBody]),
            question = this[TaskEntity.question],
            createdAt = this[TaskEntity.createdAt].toString(),
            updatedAt = this[TaskEntity.updatedAt].toString()
        )
}