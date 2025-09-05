package com.inRussian.repositories

import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.requests.content.*
import com.inRussian.tables.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


@Serializable
data class ThemeContents(
    val theme: Theme,
    val childThemes: List<Theme>,
    val tasks: List<TaskModel>
)

@Serializable
data class ThemeTreeNode(
    val theme: Theme,
    val children: List<ThemeTreeNode>
)

interface ContentRepository {

    // Themes
    suspend fun createTheme(request: CreateThemeRequest): Theme
    suspend fun getTheme(themeId: String): Theme?
    suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Theme?
    suspend fun deleteTheme(themeId: String): Boolean

    // New: theme discovery in the new hierarchy
    suspend fun getThemesByCourse(courseId: String): List<Theme>
    suspend fun getThemesByTheme(parentThemeId: String): List<Theme>
    suspend fun getThemeContents(themeId: String): ThemeContents?
    suspend fun getThemeSubtree(themeId: String): ThemeTreeNode?
    suspend fun getCourseThemeTree(courseId: String): List<ThemeTreeNode>
    suspend fun getTasksByTheme(themeId: String): List<TaskModel>

    // Courses
    suspend fun createCourse(authorId: String, request: CreateCourseRequest): Course
    suspend fun getCourse(courseId: String): Course?
    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Course?
    suspend fun deleteCourse(courseId: String): Boolean
    suspend fun getAllCourses(): List<Course>

    // Reports
    suspend fun createReport(reporterId: String, request: CreateReportRequest): Report
    suspend fun getReport(reportId: String): Report?
    suspend fun deleteReport(reportId: String): Boolean
    suspend fun getAllReports(): List<Report>

    // Statistics
    suspend fun getCountStats(): CountStats
    suspend fun getCourseTasksCount(courseId: String): Long
    suspend fun getThemeTasksCount(themeId: String): Long
}

class ExposedContentRepository : ContentRepository {


    private val taskJson = Json {
        serializersModule = SerializersModule {
            polymorphic(TaskBody::class) {
                subclass(TaskBody.AudioConnectTask::class, TaskBody.AudioConnectTask.serializer())
                subclass(TaskBody.TextConnectTask::class, TaskBody.TextConnectTask.serializer())
                subclass(TaskBody.TextInputTask::class, TaskBody.TextInputTask.serializer())
                subclass(TaskBody.TextInputWithVariantTask::class, TaskBody.TextInputWithVariantTask.serializer())
                subclass(TaskBody.ImageConnectTask::class, TaskBody.ImageConnectTask.serializer())
                subclass(TaskBody.ListenAndSelect::class, TaskBody.ListenAndSelect.serializer())
                subclass(TaskBody.ImageAndSelect::class, TaskBody.ImageAndSelect.serializer())
                subclass(TaskBody.ConstructSentenceTask::class, TaskBody.ConstructSentenceTask.serializer())
                subclass(TaskBody.SelectWordsTask::class, TaskBody.SelectWordsTask.serializer())
            }
            PolymorphicSerializer(TaskBody::class)
        }
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // ---------- Mappers ----------

    private fun ResultRow.toCourse() = Course(
        id = this[Courses.id].toString(),
        name = this[Courses.name],
        description = this[Courses.description],
        authorId = this[Courses.authorId].toString(),
        authorUrl = this[Courses.authorUrl],
        language = this[Courses.language],
        isPublished = this[Courses.isPublished],
        createdAt = this[Courses.createdAt].toString(),
        updatedAt = this[Courses.updatedAt].toString(),
        posterId = this[Courses.posterId]?.toString()
    )

    private fun ResultRow.toReport() = Report(
        id = this[Reports.id].toString(),
        description = this[Reports.description],
        taskId = this[Reports.taskId].toString(),
        reporterId = this[Reports.reporterId].toString(),
        createdAt = this[Reports.createdAt].toString()
    )

    private fun ResultRow.toTheme() = Theme(
        id = this[Themes.id].toString(),
        courseId = this[Themes.courseId].toString(),
        parentThemeId = this[Themes.parentThemeId]?.toString(),
        name = this[Themes.name],
        description = this[Themes.description],
        position = this[Themes.position],
        createdAt = this[Themes.createdAt].toString()
    )

    // ---------- Internal helpers for tasks (same logic as TaskRepository) ----------

    private fun loadTaskTypes(taskId: UUID): List<TaskType> =
        TaskToTypes
            .join(
                TaskTypes,
                JoinType.INNER,
                onColumn = TaskToTypes.typeName,
                otherColumn = TaskTypes.name
            )
            .selectAll().where { TaskToTypes.task eq taskId }
            .map { row ->
                val cleaned = row[TaskToTypes.typeName].trim().trim('"')
                enumValues<TaskType>().first { it.name.equals(cleaned, ignoreCase = true) }
            }

    private fun loadTaskModelById(taskId: UUID): TaskModel? {
        val taskEntity = TaskEntity
            .selectAll().where { TaskEntity.id eq taskId }
            .singleOrNull() ?: return null

        val taskBody = taskJson.decodeFromJsonElement(
            PolymorphicSerializer(TaskBody::class),
            taskEntity[TaskEntity.taskBody]
        )
        val types = loadTaskTypes(taskId)

        return TaskModel(
            id = taskEntity[TaskEntity.id].value.toString(),
            taskType = types,
            taskBody = taskBody,
            question = taskEntity[TaskEntity.question],
            createdAt = taskEntity[TaskEntity.createdAt].toString(),
            updatedAt = taskEntity[TaskEntity.updatedAt].toString()
        )
    }

    private fun listTasksByTheme(themeUuid: UUID): List<TaskModel> {
        val ids = TaskEntity
            .selectAll().where { TaskEntity.themeId eq themeUuid }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)
            .map { it[TaskEntity.id].value }

        return ids.mapNotNull { id ->
            loadTaskModelById(id)
        }
    }

    private fun collectSubtreeThemeIds(rootId: UUID): List<UUID> = transaction {
        val result = LinkedHashSet<UUID>()
        val queue = ArrayDeque<UUID>()
        result.add(rootId)
        queue.add(rootId)

        while (queue.isNotEmpty()) {
            val batch = queue.toList()
            queue.clear()

            Themes
                .selectAll().where { Themes.parentThemeId inList batch }
                .forEach { row ->
                    val childId = row[Themes.id].value
                    if (result.add(childId)) queue.add(childId)
                }
        }
        result.toList()
    }

    private fun deleteTasksAndRelated(taskIds: List<EntityID<UUID>>) {
        if (taskIds.isEmpty()) return
        TaskEntity.deleteWhere { TaskEntity.id inList taskIds }
    }

    // ---------- Themes ----------

    override suspend fun createTheme(request: CreateThemeRequest): Theme = transaction {
        val parentId = request.parentThemeId?.let(UUID::fromString)

        val courseUuid: UUID = if (parentId != null) {
            Themes
                .selectAll().where { Themes.id eq parentId }
                .limit(1)
                .firstOrNull()?.get(Themes.courseId)?.value
                ?: throw IllegalArgumentException("Parent theme not found: $parentId")
        } else {
            request.courseId?.let(UUID::fromString)
                ?: throw IllegalArgumentException("courseId is required for root themes")
        }

        if (parentId != null) {
            val parentHasTasks = TaskEntity
                .selectAll().where { TaskEntity.themeId eq parentId }
                .limit(1)
                .any()
            if (parentHasTasks) {
                throw IllegalStateException("Cannot create a child theme under a theme that already has tasks")
            }
        }

        val pos = (request.position ?: run {
            val siblingCount = if (parentId != null) {
                Themes.selectAll().where { Themes.parentThemeId eq parentId }.count()
            } else {
                Themes.selectAll().where { (Themes.courseId eq courseUuid) and Themes.parentThemeId.isNull() }.count()
            }
            siblingCount + 1
        }).toInt()

        val newId = Themes.insertAndGetId {
            it[name] = request.name
            it[description] = request.description
            it[courseId] = EntityID(courseUuid, Courses)
            it[parentThemeId] = parentId?.let { pid -> EntityID(pid, Themes) }
            it[position] = pos
        }

        Themes.selectAll().where { Themes.id eq newId }.single().toTheme()
    }

    override suspend fun getTheme(themeId: String): Theme? = transaction {
        Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull()
            ?.toTheme()
    }

    override suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Theme? {
        val themeUuid = UUID.fromString(themeId)
        transaction {
            Themes.update({ Themes.id eq themeUuid }) { row ->
                request.name?.let { row[Themes.name] = it }
                request.description?.let { row[Themes.description] = it }
                request.position?.let { row[Themes.position] = it }
                request.parentThemeId?.let { pid ->
                    val newParentUuid = UUID.fromString(pid)
                    val parentHasTasks = TaskEntity
                        .selectAll().where { TaskEntity.themeId eq newParentUuid }
                        .limit(1)
                        .any()
                    if (parentHasTasks) {
                        throw IllegalStateException("Cannot move theme under a parent that already has tasks")
                    }
                    row[Themes.parentThemeId] = EntityID(newParentUuid, Themes)
                }
            }
        }
        return getTheme(themeId)
    }

    override suspend fun deleteTheme(themeId: String): Boolean = transaction {
        val rootId = UUID.fromString(themeId)
        val subtreeIds = collectSubtreeThemeIds(rootId)

        val taskIds = TaskEntity
            .selectAll().where { TaskEntity.themeId inList subtreeIds }
            .map { it[TaskEntity.id] }

        deleteTasksAndRelated(taskIds)

        Themes.deleteWhere { Themes.id inList subtreeIds } > 0
    }

    override suspend fun getThemesByCourse(courseId: String): List<Theme> = transaction {
        Themes
            .selectAll().where { (Themes.courseId eq UUID.fromString(courseId)) and (Themes.parentThemeId.isNull()) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun getThemesByTheme(parentThemeId: String): List<Theme> = transaction {
        Themes
            .selectAll().where { Themes.parentThemeId eq UUID.fromString(parentThemeId) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun getTasksByTheme(themeId: String): List<TaskModel> = transaction {
        listTasksByTheme(UUID.fromString(themeId))
    }

    override suspend fun getThemeContents(themeId: String): ThemeContents? = transaction {
        val themeRow = Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull() ?: return@transaction null

        val theme = themeRow.toTheme()

        val children = Themes
            .selectAll().where { Themes.parentThemeId eq UUID.fromString(theme.id) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }

        val tasks = listTasksByTheme(UUID.fromString(theme.id))

        ThemeContents(theme = theme, childThemes = children, tasks = tasks)
    }

    override suspend fun getThemeSubtree(themeId: String): ThemeTreeNode? = transaction {
        val root = Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull()
            ?.toTheme()
            ?: return@transaction null

        fun buildNode(current: Theme): ThemeTreeNode {
            val children = Themes
                .selectAll().where { Themes.parentThemeId eq UUID.fromString(current.id) }
                .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
                .map { it.toTheme() }
            val childNodes = children.map { buildNode(it) }
            return ThemeTreeNode(theme = current, children = childNodes)
        }

        buildNode(root)
    }

    override suspend fun getCourseThemeTree(courseId: String): List<ThemeTreeNode> = transaction {
        val roots = Themes
            .selectAll().where { (Themes.courseId eq UUID.fromString(courseId)) and (Themes.parentThemeId.isNull()) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }

        fun buildNode(current: Theme): ThemeTreeNode {
            val children = Themes
                .selectAll().where { Themes.parentThemeId eq UUID.fromString(current.id) }
                .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
                .map { it.toTheme() }
            val childNodes = children.map { buildNode(it) }
            return ThemeTreeNode(theme = current, children = childNodes)
        }

        roots.map { buildNode(it) }
    }

    // ---------- Courses ----------

    override suspend fun createCourse(authorId: String, request: CreateCourseRequest): Course = transaction {
        val courseId = Courses.insertAndGetId {
            it[name] = request.name
            it[description] = request.description
            it[Courses.authorId] = UUID.fromString(authorId)
            it[posterId] = request.coursePoster?.let(UUID::fromString)
            it[authorUrl] = request.authorUrl
            it[language] = request.language
            it[isPublished] = request.isPublished
            it[updatedAt] = CurrentTimestamp
        }
        Courses.selectAll().where { Courses.id eq courseId }.single().toCourse()
    }

    override suspend fun getCourse(courseId: String): Course? = transaction {
        Courses
            .selectAll().where { Courses.id eq UUID.fromString(courseId) }
            .singleOrNull()
            ?.toCourse()
    }

    override suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Course? {
        val courseUuid = UUID.fromString(courseId)
        transaction {
            Courses.update({ Courses.id eq courseUuid }) {
                request.name?.let { name -> it[Courses.name] = name }
                request.description?.let { desc -> it[Courses.description] = desc }
                request.authorUrl?.let { url -> it[Courses.authorUrl] = url }
                request.language?.let { lang -> it[Courses.language] = lang }
                request.isPublished?.let { published -> it[Courses.isPublished] = published }
                request.coursePoster?.let { poster -> it[Courses.posterId] = UUID.fromString(poster) }
                it[Courses.updatedAt] = CurrentTimestamp
            }
        }
        return getCourse(courseId)
    }

    override suspend fun deleteCourse(courseId: String): Boolean = transaction {
        val courseUuid = UUID.fromString(courseId)

        val themeIds: List<UUID> = Themes
            .selectAll().where { Themes.courseId eq courseUuid }
            .map { it[Themes.id].value }

        if (themeIds.isNotEmpty()) {
            val taskIds = TaskEntity
                .selectAll().where { TaskEntity.themeId inList themeIds }
                .map { it[TaskEntity.id] }
            deleteTasksAndRelated(taskIds)
            Themes.deleteWhere { Themes.id inList themeIds }
        }

        Courses.deleteWhere { Courses.id eq courseUuid } > 0
    }

    override suspend fun getAllCourses(): List<Course> = transaction {
        Courses
            .selectAll()
            .orderBy(Courses.createdAt, SortOrder.DESC)
            .map { it.toCourse() }
    }

    // ---------- Reports ----------

    override suspend fun createReport(reporterId: String, request: CreateReportRequest): Report = transaction {
        val reportId = Reports.insertAndGetId {
            it[description] = request.description
            it[taskId] = UUID.fromString(request.taskId)
            it[Reports.reporterId] = UUID.fromString(reporterId)
        }

        Reports.selectAll().where { Reports.id eq reportId }.single().toReport()
    }

    override suspend fun getReport(reportId: String): Report? = transaction {
        Reports
            .selectAll().where { Reports.id eq UUID.fromString(reportId) }
            .singleOrNull()
            ?.toReport()
    }

    override suspend fun deleteReport(reportId: String): Boolean = transaction {
        Reports.deleteWhere { Reports.id eq UUID.fromString(reportId) } > 0
    }

    override suspend fun getAllReports(): List<Report> = transaction {
        Reports
            .selectAll()
            .orderBy(Reports.createdAt, SortOrder.DESC)
            .map { it.toReport() }
    }

    // ---------- Statistics ----------

    override suspend fun getCountStats(): CountStats = transaction {
        val coursesCount = Courses.selectAll().count()
        val themesCount = Themes.selectAll().count()
        val tasksCount = TaskEntity.selectAll().count()
        CountStats(coursesCount, themesCount, tasksCount)
    }

    override suspend fun getCourseTasksCount(courseId: String): Long = transaction {
        (TaskEntity innerJoin Themes)
            .selectAll().where { Themes.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getThemeTasksCount(themeId: String): Long = transaction {
        val themeUuid = UUID.fromString(themeId)
        val subtreeIds = collectSubtreeThemeIds(themeUuid)
        if (subtreeIds.isEmpty()) 0L
        else TaskEntity.selectAll().where { TaskEntity.themeId inList subtreeIds }.count()
    }
}