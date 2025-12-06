package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.tasks.TaskBody
import com.inRussian.models.tasks.TaskModel
import com.inRussian.models.tasks.TaskType
import com.inRussian.requests.content.CreateTaskModelRequest
import com.inRussian.requests.content.UpdateTaskModelRequest

import com.inRussian.tables.TaskEntity
import com.inRussian.tables.TaskToTypes
import com.inRussian.tables.TaskTypes
import com.inRussian.tables.Themes
import kotlinx.serialization.PolymorphicSerializer
import org.jetbrains.exposed.sql.*
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import kotlin.text.insert
import kotlin.text.set


interface TasksRepository {
    suspend fun createTask(request: CreateTaskModelRequest): TaskModel
    suspend fun updateTask(taskId: UUID, request: UpdateTaskModelRequest): TaskModel?
    suspend fun deleteTask(taskId: UUID): Boolean
    suspend fun getTaskById(taskId: UUID): TaskModel
    suspend fun getTaskByThemeId(themeId: UUID): List<TaskModel>

    suspend fun listByTheme(themeId: UUID): List<TaskModel>
    suspend fun deleteByThemes(themeIds: List<UUID>)
    suspend fun themeHasTasks(themeId: UUID): Boolean
    suspend fun loadTaskModelById(taskId: UUID): TaskModel?
    suspend fun countByCourse(courseId: UUID): Long
    suspend fun countByThemes(themeIds: List<UUID>): Long

    // tx-local helpers (call only inside an existing transaction; no suspend)
    fun listByThemeTx(themeId: UUID): List<TaskModel>
    fun deleteByThemesTx(themeIds: List<UUID>)
    fun themeHasTasksTx(themeId: UUID): Boolean
    fun selectTaskDtoByIdTx(taskId: UUID): TaskModel?
    suspend fun cloneTask(taskId: UUID, newThemeId: UUID): TaskModel
}

class ExposedTasksRepository(
    private val json: Json
) : TasksRepository {

    // ---------- CRUD ----------

    override suspend fun createTask(request: CreateTaskModelRequest): TaskModel = dbQuery {
        val taskBodyJson = json.encodeToJsonElement(request.taskBody)

        val insertedId = TaskEntity.insertAndGetId { task ->
            task[themeId] = UUID.fromString(request.themeId)
            task[taskBody] = taskBodyJson
            task[question] = request.question
        }.value

        request.taskTypes.forEach { taskType ->
            TaskTypes.insertIgnore { it[name] = taskType.name }
            TaskToTypes.insert {
                it[task] = insertedId
                it[typeName] = taskType.name
            }
        }

        selectTaskDtoByIdTx(insertedId)
            ?: throw IllegalStateException("Created task not found: $insertedId")
    }

    override suspend fun updateTask(taskId: UUID, request: UpdateTaskModelRequest): TaskModel? = dbQuery {
        val updated = TaskEntity.update({ TaskEntity.id eq taskId }) { st ->
            request.themeId?.let { st[TaskEntity.themeId] = UUID.fromString(it) }
            request.taskBody?.let { st[TaskEntity.taskBody] = json.encodeToJsonElement(it) }
            request.question?.let { st[TaskEntity.question] = it }
            st[TaskEntity.updatedAt] = CurrentTimestamp
        }
        if (updated == 0) return@dbQuery null

        request.taskTypes?.let { newTypes ->
            TaskToTypes.deleteWhere { TaskToTypes.task eq taskId }
            newTypes.forEach { taskType ->
                TaskTypes.insertIgnore { it[name] = taskType.name }
                TaskToTypes.insert {
                    it[task] = taskId
                    it[typeName] = taskType.name
                }
            }
        }

        selectTaskDtoByIdTx(taskId)
    }

    override suspend fun deleteTask(taskId: UUID): Boolean = dbQuery {
        TaskToTypes.deleteWhere { TaskToTypes.task eq taskId }
        TaskEntity.deleteWhere { TaskEntity.id eq taskId } > 0
    }

    override suspend fun getTaskById(taskId: UUID): TaskModel = dbQuery {
        selectTaskDtoByIdTx(taskId) ?: throw IllegalStateException("Task not found: $taskId")
    }

    override suspend fun getTaskByThemeId(themeId: UUID): List<TaskModel> = dbQuery {
        TaskEntity.selectAll()
            .where { TaskEntity.themeId eq themeId }
            .mapNotNull { selectTaskDtoByIdTx(it[TaskEntity.id].value) }
    }

    // ---------- Counts / queries ----------

    override suspend fun listByTheme(themeId: UUID): List<TaskModel> = dbQuery { listByThemeTx(themeId) }

    override suspend fun deleteByThemes(themeIds: List<UUID>) = dbQuery { deleteByThemesTx(themeIds) }

    override suspend fun themeHasTasks(themeId: UUID): Boolean = dbQuery { themeHasTasksTx(themeId) }

    override suspend fun loadTaskModelById(taskId: UUID): TaskModel? = dbQuery {
        TaskEntity.selectAll().where { TaskEntity.id eq taskId }.singleOrNull()?.let { loadTaskModelRowTx(it) }
    }

    override suspend fun countByCourse(courseId: UUID): Long = dbQuery {
        (TaskEntity innerJoin Themes)
            .selectAll()
            .where { Themes.courseId eq courseId }
            .count()
    }

    override suspend fun countByThemes(themeIds: List<UUID>): Long = dbQuery {
        if (themeIds.isEmpty()) 0 else TaskEntity.selectAll().where { TaskEntity.themeId inList themeIds }.count()
    }

    // ---------- tx-local helpers ----------

    override fun listByThemeTx(themeId: UUID): List<TaskModel> =
        TaskEntity.selectAll()
            .where { TaskEntity.themeId eq themeId }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)
            .mapNotNull { loadTaskModelRowTx(it) }

    override fun deleteByThemesTx(themeIds: List<UUID>) {
        if (themeIds.isEmpty()) return
        TaskEntity.deleteWhere { TaskEntity.themeId inList themeIds }
    }

    override fun themeHasTasksTx(themeId: UUID): Boolean =
        TaskEntity.selectAll().where { TaskEntity.themeId eq themeId }.limit(1).any()

    override fun selectTaskDtoByIdTx(taskId: UUID): TaskModel? {
        val row = TaskEntity.selectAll().where { TaskEntity.id eq taskId }.singleOrNull() ?: return null
        return loadTaskModelRowTx(row)
    }

    // ---------- private tx-local helpers ----------

    private fun loadTaskTypesTx(taskId: UUID): List<TaskType> =
        TaskToTypes
            .join(TaskTypes, JoinType.INNER, onColumn = TaskToTypes.typeName, otherColumn = TaskTypes.name)
            .selectAll()
            .where { TaskToTypes.task eq taskId }
            .map { row ->
                val cleaned = row[TaskToTypes.typeName].trim().trim('"')
                enumValues<TaskType>().first { it.name.equals(cleaned, ignoreCase = true) }
            }

    private fun loadTaskModelRowTx(row: ResultRow): TaskModel? {
        val taskId = row[TaskEntity.id].value
        val taskBody = json.decodeFromJsonElement(
            PolymorphicSerializer(TaskBody::class),
            row[TaskEntity.taskBody]
        )
        val types = loadTaskTypesTx(taskId)
        return TaskModel(
            id = taskId.toString(),
            taskType = types,
            taskBody = taskBody,
            question = row[TaskEntity.question],
            createdAt = row[TaskEntity.createdAt].toString(),
            updatedAt = row[TaskEntity.updatedAt].toString()
        )
    }
    override suspend fun cloneTask(taskId: UUID, newThemeId: UUID): TaskModel = dbQuery {
        val original = selectTaskDtoByIdTx(taskId)
            ?: throw NoSuchElementException("Task not found: $taskId")

        val taskBodyJson = json.encodeToJsonElement(
            PolymorphicSerializer(TaskBody::class),
            original.taskBody
        )

        val newId = TaskEntity.insertAndGetId {
            it[themeId] = newThemeId
            it[taskBody] = taskBodyJson
            it[question] = original.question ?: ""
        }.value

        original.taskType.forEach { taskType ->
            TaskTypes.insertIgnore { it[name] = taskType.name }
            TaskToTypes.insert {
                it[task] = newId
                it[typeName] = taskType.name
            }
        }

        selectTaskDtoByIdTx(newId)
            ?: throw IllegalStateException("Cloned task not found: $newId")
    }
}