package com.inRussian.responses

import com.inRussian.models.content.TaskWithDetails
import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskType
import com.inRussian.requests.content.CreateTaskModelRequest
import com.inRussian.tables.TaskModel.courseId
import com.inRussian.tables.TaskModel
import com.inRussian.tables.TaskToTypes
import com.inRussian.tables.TaskTypes
import com.inRussian.tables.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class TaskRepository {
    fun createTask(request: CreateTaskModelRequest): com.inRussian.models.tasks.TaskModel = transaction {

        val taskBodyJson: JsonElement = json.encodeToJsonElement(request.taskBody)

        val insertedId = TaskModel.insertAndGetId { row ->
            row[TaskModel.courseId] = UUID.fromString(request.courseId)
            row[TaskModel.taskBody] = taskBodyJson
        }

        request.taskType.forEach { taskType ->
            val exists = TaskTypes
                .select { TaskTypes.name eq taskType.name }
                .limit(1)
                .count() > 0

            if (!exists) {
                TaskTypes.insert { it[name] =  taskType.name }
            }

            // Связываем задачу с типом
            TaskToTypes.insert {
                it[task] = insertedId
                it[typeName] = typeName
            }
        }

        selectTaskDtoById(insertedId.value)
            ?: throw IllegalStateException("Created task not found: $insertedId")
    }

    fun getTaskById(id: UUID): com.inRussian.models.tasks.TaskModel? = transaction {
        selectTaskDtoById(id)
    }

    // Получить все задачи (пример с группировкой типов)
    fun getAllTasks(): List<com.inRussian.models.tasks.TaskModel> = transaction {
        val rows = TaskModel.selectAll().toList()
        if (rows.isEmpty()) return@transaction emptyList()

        // получаем все связи для этих задач одним запросом
        val ids = rows.map { it[TaskModel.id].value }
        val pairs = TaskToTypes.select { TaskToTypes.task inList ids }
            .map { it[TaskToTypes.task].value to it[TaskToTypes.typeName] }

        val typesByTask: Map<UUID, List<TaskType>> =
            pairs.groupBy({ it.first }, { TaskType.valueOf(it.second) })

        rows.map { row ->
            val id = row[TaskModel.id].value
            val types = typesByTask[id] ?: emptyList()
            row.toTaskDto(types)
        }
    }

    // Вспомогательная функция: читает одну задачу + её типы и мапит в DTO
    private fun selectTaskDtoById(id: UUID): com.inRussian.models.tasks.TaskModel? {
        val row = TaskModel.select { TaskModel.id eq id }.singleOrNull() ?: return null
        val types = TaskToTypes.select { TaskToTypes.task eq id }
            .map { TaskType.valueOf(it[TaskToTypes.typeName]) }
        return row.toTaskDto(types)
    }

    // Mapper: ResultRow -> DTO (тут task types передаются извне, чтобы избежать N+1)
    private fun ResultRow.toTaskDto(types: List<TaskType>): com.inRussian.models.tasks.TaskModel =
        com.inRussian.models.tasks.TaskModel(
            id = this[TaskModel.id].value.toString(),
            taskType = types,
            taskBody = this[TaskModel.taskBody],
            createdAt = this[TaskModel.createdAt].toString(),
            updatedAt = this[TaskModel.updatedAt].toString()
        )
}