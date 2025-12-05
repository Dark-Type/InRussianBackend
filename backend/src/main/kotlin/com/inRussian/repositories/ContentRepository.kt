package com.inRussian.repositories

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.requests.content.*
import com.inRussian.tables.*
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.name
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.get
import kotlin.collections.orEmpty
import kotlin.collections.set
import kotlin.text.format
import kotlin.text.get
import kotlin.text.insert
import kotlin.text.orEmpty
import kotlin.text.set
import kotlin.text.toInt
import kotlin.toString

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

@Serializable
data class ImportReport(
    val createdThemes: Int,
    val createdTasks: Int,
    val skippedThemes: Int,
    val skippedTasks: Int
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

    // Export / Import
    suspend fun exportCourseJson(courseId: String, sinceIsoUtc: String? = null): String


    suspend fun importCourseJson(
        json: String,
        targetCourseId: String? = null,
        createIfMissing: Boolean = true,
        languageOverride: String? = null,
        addOnly: Boolean = true
    ): ImportReport

    suspend fun cloneCourseStructure(sourceCourseId: String, newLanguage: String, newCourseName: String): Course
}

class ExposedContentRepository : ContentRepository {

    private val taskJson = Json {
        serializersModule = SerializersModule {
            polymorphic(TaskBody::class) {
                subclass(TaskBody.ContentBlocks::class, TaskBody.ContentBlocks.serializer())
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

    // ---------- Internal helpers for tasks ----------

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

    private fun collectSubtreeThemeIds(rootId: UUID): List<UUID> {
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
        return result.toList()
    }

    private fun deleteTasksAndRelated(taskIds: List<EntityID<UUID>>) {
        if (taskIds.isEmpty()) return
        TaskEntity.deleteWhere { TaskEntity.id inList taskIds }
    }

    // ---------- Themes ----------

    override suspend fun createTheme(request: CreateThemeRequest): Theme = dbQuery {
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

    override suspend fun getTheme(themeId: String): Theme? = dbQuery {
        Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull()
            ?.toTheme()
    }

    override suspend fun updateTheme(themeId: String, request: UpdateThemeRequest): Theme? = dbQuery {
        val themeUuid = UUID.fromString(themeId)
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

        // Fetch and return updated theme
        Themes.selectAll().where { Themes.id eq themeUuid }
            .singleOrNull()
            ?.toTheme()
    }

    override suspend fun deleteTheme(themeId: String): Boolean = dbQuery {
        val rootId = UUID.fromString(themeId)
        val subtreeIds = collectSubtreeThemeIds(rootId)

        val taskIds = TaskEntity
            .selectAll().where { TaskEntity.themeId inList subtreeIds }
            .map { it[TaskEntity.id] }

        deleteTasksAndRelated(taskIds)

        Themes.deleteWhere { Themes.id inList subtreeIds } > 0
    }

    override suspend fun getThemesByCourse(courseId: String): List<Theme> = dbQuery {
        Themes
            .selectAll().where { (Themes.courseId eq UUID.fromString(courseId)) and (Themes.parentThemeId.isNull()) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun getThemesByTheme(parentThemeId: String): List<Theme> = dbQuery {
        Themes
            .selectAll().where { Themes.parentThemeId eq UUID.fromString(parentThemeId) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun getTasksByTheme(themeId: String): List<TaskModel> = dbQuery {
        listTasksByTheme(UUID.fromString(themeId))
    }

    override suspend fun getThemeContents(themeId: String): ThemeContents? = dbQuery {
        val themeRow = Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull() ?: return@dbQuery null

        val theme = themeRow.toTheme()

        val children = Themes
            .selectAll().where { Themes.parentThemeId eq UUID.fromString(theme.id) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }

        val tasks = listTasksByTheme(UUID.fromString(theme.id))

        ThemeContents(theme = theme, childThemes = children, tasks = tasks)
    }

    override suspend fun getThemeSubtree(themeId: String): ThemeTreeNode? = dbQuery {
        val root = Themes
            .selectAll().where { Themes.id eq UUID.fromString(themeId) }
            .singleOrNull()
            ?.toTheme()
            ?: return@dbQuery null

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

    override suspend fun getCourseThemeTree(courseId: String): List<ThemeTreeNode> = dbQuery {
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

    override suspend fun createCourse(authorId: String, request: CreateCourseRequest): Course = dbQuery {
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

    override suspend fun getCourse(courseId: String): Course? = dbQuery {
        Courses
            .selectAll().where { Courses.id eq UUID.fromString(courseId) }
            .singleOrNull()
            ?.toCourse()
    }

    override suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): Course? = dbQuery {
        val courseUuid = UUID.fromString(courseId)
        Courses.update({ Courses.id eq courseUuid }) {
            request.name?.let { name -> it[Courses.name] = name }
            request.description?.let { desc -> it[Courses.description] = desc }
            request.authorUrl?.let { url -> it[Courses.authorUrl] = url }
            request.language?.let { lang -> it[Courses.language] = lang }
            request.isPublished?.let { published -> it[Courses.isPublished] = published }
            request.coursePoster?.let { poster -> it[Courses.posterId] = UUID.fromString(poster) }
            it[Courses.updatedAt] = CurrentTimestamp
        }

        // Fetch and return updated course
        Courses.selectAll().where { Courses.id eq courseUuid }
            .singleOrNull()
            ?.toCourse()
    }

    override suspend fun deleteCourse(courseId: String): Boolean = dbQuery {
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

    override suspend fun getAllCourses(): List<Course> = dbQuery {
        Courses
            .selectAll()
            .orderBy(Courses.createdAt, SortOrder.DESC)
            .map { it.toCourse() }
    }

    // ---------- Reports ----------

    override suspend fun createReport(reporterId: String, request: CreateReportRequest): Report = dbQuery {
        val reportId = Reports.insertAndGetId {
            it[description] = request.description
            it[taskId] = UUID.fromString(request.taskId)
            it[Reports.reporterId] = UUID.fromString(reporterId)
        }

        Reports.selectAll().where { Reports.id eq reportId }.single().toReport()
    }

    override suspend fun getReport(reportId: String): Report? = dbQuery {
        Reports
            .selectAll().where { Reports.id eq UUID.fromString(reportId) }
            .singleOrNull()
            ?.toReport()
    }

    override suspend fun deleteReport(reportId: String): Boolean = dbQuery {
        Reports.deleteWhere { Reports.id eq UUID.fromString(reportId) } > 0
    }

    override suspend fun getAllReports(): List<Report> = dbQuery {
        Reports
            .selectAll()
            .orderBy(Reports.createdAt, SortOrder.DESC)
            .map { it.toReport() }
    }

    // ---------- Statistics ----------

    override suspend fun getCountStats(): CountStats = dbQuery {
        val coursesCount = Courses.selectAll().count()
        val themesCount = Themes.selectAll().count()
        val tasksCount = TaskEntity.selectAll().count()
        CountStats(coursesCount, themesCount, tasksCount)
    }

    override suspend fun getCourseTasksCount(courseId: String): Long = dbQuery {
        (TaskEntity innerJoin Themes)
            .selectAll().where { Themes.courseId eq UUID.fromString(courseId) }
            .count()
    }

    override suspend fun getThemeTasksCount(themeId: String): Long = dbQuery {
        val themeUuid = UUID.fromString(themeId)
        val subtreeIds = collectSubtreeThemeIds(themeUuid)
        if (subtreeIds.isEmpty()) 0L
        else TaskEntity.selectAll().where { TaskEntity.themeId inList subtreeIds }.count()
    }

    // --- Export ---
    override suspend fun exportCourseJson(courseId: String, sinceIsoUtc: String?): String = dbQuery {
        val courseUuid = UUID.fromString(courseId)

        val courseRow = Courses
            .selectAll().where { Courses.id eq courseUuid }
            .singleOrNull() ?: throw IllegalArgumentException("Course not found: $courseId")

        val courseObj = buildJsonObject {
            put("id", courseRow[Courses.id].value.toString())
            put("name", courseRow[Courses.name])
            put("language", courseRow[Courses.language])
            put("createdAt", courseRow[Courses.createdAt].toString())
            put("updatedAt", courseRow[Courses.updatedAt].toString())
        }

        val themeRows = Themes
            .selectAll().where { Themes.courseId eq courseUuid }
            .orderBy(Themes.createdAt to SortOrder.ASC)
            .toList()

        val byId = themeRows.associateBy { it[Themes.id].value }
        fun buildPathNames(themeId: UUID): List<String> {
            val names = ArrayDeque<String>()
            var cur: UUID? = themeId
            while (cur != null) {
                val r = byId[cur] ?: break
                names.addFirst(r[Themes.name])
                cur = r[Themes.parentThemeId]?.value
            }
            return names.toList()
        }

        val sinceLdt: java.time.LocalDateTime? = sinceIsoUtc?.let {
            val inst = Instant.parse(it)
            java.time.LocalDateTime.ofInstant(inst, ZoneOffset.UTC)
        }

        val tasksByTheme = mutableMapOf<UUID, MutableList<TaskModel>>()
        val themeIds = themeRows.map { it[Themes.id].value }

        val tasksQuery = TaskEntity
            .selectAll()
            .where { TaskEntity.themeId inList themeIds }
            .apply {
                sinceIsoUtc?.let { iso ->
                    andWhere { TaskEntity.updatedAt greaterEq Instant.parse(iso) }

                }
            }
            .orderBy(TaskEntity.createdAt to SortOrder.ASC)

        val taskIdsOrdered: List<UUID> = tasksQuery.map { it[TaskEntity.id].value }

        val taskThemePairs: Map<UUID, UUID> = TaskEntity
            .selectAll().where { TaskEntity.id inList taskIdsOrdered }
            .associate { it[TaskEntity.id].value to it[TaskEntity.themeId].value }

        val modelsById: Map<UUID, TaskModel> =
            taskIdsOrdered.mapNotNull { id -> loadTaskModelById(id)?.let { id to it } }.toMap()

        taskThemePairs.forEach { (tid, themeUuid) ->
            val list = tasksByTheme.getOrPut(themeUuid) { mutableListOf() }
            modelsById[tid]?.let { list.add(it) }
        }

        val themesJson = buildJsonArray {
            for (row in themeRows) {
                val themeId = row[Themes.id].value
                val tasks = tasksByTheme[themeId].orEmpty()

                add(
                    buildJsonObject {
                        putJsonArray("path") { buildPathNames(themeId).forEach { add(it) } }
                        put("name", row[Themes.name])
                        row[Themes.description]?.let { put("description", it) }
                        put("position", row[Themes.position])
                        put("createdAt", row[Themes.createdAt].toString())

                        putJsonArray("tasks") {
                            tasks.forEach { tm ->
                                add(
                                    buildJsonObject {
                                        put("question", tm.question)
                                        putJsonArray("types") { tm.taskType.forEach { add(it.name) } }
                                        put(
                                            "body",
                                            taskJson.encodeToJsonElement(
                                                PolymorphicSerializer(TaskBody::class),
                                                tm.taskBody
                                            )
                                        )
                                        put("createdAt", tm.createdAt)
                                        put("updatedAt", tm.updatedAt)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }

        buildJsonObject {
            put("version", 1)
            put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            sinceIsoUtc?.let { put("since", it) }
            put("course", courseObj)
            put("themes", themesJson)
        }.toString() // ВАЖНО: вернуть строку последним выражением
    }

    // --- Import (части с modern DSL и мелкими фиксам) ---
    override suspend fun importCourseJson(
        json: String,
        targetCourseId: String?,
        createIfMissing: Boolean,
        languageOverride: String?,
        addOnly: Boolean
    ): ImportReport = dbQuery {
        val root = Json.parseToJsonElement(json).jsonObject
        val courseJson = root["course"]?.jsonObject
        val srcCourseName = courseJson?.get("name")?.jsonPrimitive?.contentOrNull ?: "Imported Course"
        val srcCourseLang = courseJson?.get("language")?.jsonPrimitive?.contentOrNull

        val courseUuid: UUID = when {
            targetCourseId != null -> UUID.fromString(targetCourseId)
            createIfMissing -> {
                Courses.insertAndGetId {
                    it[name] = srcCourseName
                    it[description] = courseJson?.get("description")?.jsonPrimitive?.contentOrNull
                    it[authorId] = courseJson?.get("authorId")?.jsonPrimitive?.contentOrNull?.let(UUID::fromString)
                        ?: UUID.randomUUID()
                    it[authorUrl] = courseJson?.get("authorUrl")?.jsonPrimitive?.contentOrNull
                    it[language] = languageOverride ?: srcCourseLang ?: "en"
                    it[isPublished] = false
                }.value
            }

            else -> throw IllegalArgumentException("targetCourseId is null and createIfMissing=false")
        }

        fun findTheme(courseId: UUID, parentId: UUID?, name: String): ResultRow? {
            return Themes
                .selectAll().where {
                    (Themes.courseId eq courseId) and (Themes.parentThemeId eq parentId) and (Themes.name eq name)
                }
                .limit(1)
                .singleOrNull()
        }

        fun createTheme(
            courseId: UUID,
            parentId: UUID?,
            name: String,
            description: String?,
            position: Int?
        ): ResultRow {
            val pos = position ?: (
                    Themes.selectAll().where { (Themes.courseId eq courseId) and (Themes.parentThemeId eq parentId) }
                        .count()
                        .toInt() + 1
                    )
            val id = Themes.insertAndGetId {
                it[Themes.courseId] = EntityID(courseId, Courses)
                it[Themes.parentThemeId] = parentId?.let { pid -> EntityID(pid, Themes) }
                it[Themes.name] = name
                it[Themes.description] = description
                it[Themes.position] = pos
            }
            return Themes.selectAll().where { Themes.id eq id }.single()
        }

        fun findOrCreateThemeByPath(path: List<String>, desc: String?, pos: Int?): ResultRow {
            var parent: UUID? = null
            var lastRow: ResultRow? = null
            path.forEachIndexed { idx, name ->
                val existing = findTheme(courseUuid, parent, name)
                lastRow = if (existing != null) {
                    existing
                } else {
                    createTheme(
                        courseId = courseUuid,
                        parentId = parent,
                        name = name,
                        description = if (idx == path.lastIndex) desc else null,
                        position = if (idx == path.lastIndex) pos else null
                    )
                }
                parent = lastRow!![Themes.id].value
            }
            return lastRow!!
        }

        var createdThemes = 0
        var skippedThemes = 0
        var createdTasks = 0
        var skippedTasks = 0

        val themesArr = root["themes"]?.jsonArray ?: JsonArray(emptyList())

        fun loadTaskQuestionsWithTypesByTheme(themeId: UUID): List<Pair<String, Set<String>>> {
            val ids: List<Pair<UUID, String>> = TaskEntity
                .selectAll().where { TaskEntity.themeId eq themeId }
                .map { it[TaskEntity.id].value to it[TaskEntity.question] }

            return ids.map { (tid, q) ->
                val types = loadTaskTypes(tid).map { it.name }.toSet()
                q to types
            }
        }

        themesArr.forEach { tElem ->
            val tObj = tElem.jsonObject
            val path = tObj["path"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            if (path.isEmpty()) return@forEach

            val desc = tObj["description"]?.jsonPrimitive?.contentOrNull
            val pos = tObj["position"]?.jsonPrimitive?.intOrNull

            val existedBefore = findTheme(
                courseUuid,
                parentId = path.dropLast(1).fold<_, UUID?>(null) { parent, name ->
                    findTheme(courseUuid, parent, name)?.get(Themes.id)?.value
                },
                name = path.last()
            ) != null

            val leaf = findOrCreateThemeByPath(path, desc, pos)
            if (existedBefore) skippedThemes++ else createdThemes++

            val leafId = leaf[Themes.id].value
            val tasksArr = tObj["tasks"]?.jsonArray ?: JsonArray(emptyList())

            val existingPairs = loadTaskQuestionsWithTypesByTheme(leafId).toMutableList()

            tasksArr.forEach { taskEl ->
                val taskObj = taskEl.jsonObject
                val question = taskObj["question"]?.jsonPrimitive?.content ?: return@forEach
                val typesSet: Set<String> =
                    taskObj["types"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty()

                val duplicate = existingPairs.any { (q, ts) -> q == question && ts == typesSet }
                if (duplicate) {
                    skippedTasks++
                    return@forEach
                }

                val bodyEl = taskObj["body"] ?: JsonNull
                val insertedId = TaskEntity.insertAndGetId {
                    it[TaskEntity.themeId] = leafId
                    it[TaskEntity.question] = question
                    it[TaskEntity.taskBody] = bodyEl
                }

                typesSet.forEach { typeName ->
                    val typeExists = TaskTypes
                        .selectAll().where { TaskTypes.name eq typeName }
                        .limit(1)
                        .any()
                    if (!typeExists) {
                        TaskTypes.insert { it[TaskTypes.name] = typeName }
                    }
                    TaskToTypes.insert {
                        it[TaskToTypes.task] = insertedId
                        it[TaskToTypes.typeName] = typeName
                    }
                }

                existingPairs.add(question to typesSet)
                createdTasks++
            }
        }

        ImportReport(
            createdThemes = createdThemes,
            createdTasks = createdTasks,
            skippedThemes = skippedThemes,
            skippedTasks = skippedTasks
        )
    }

    // --- Clone (приведение к selectAll().where {...}) ---
    override suspend fun cloneCourseStructure(
        sourceCourseId: String,
        newLanguage: String,
        newCourseName: String
    ): Course = dbQuery {
        val srcId = UUID.fromString(sourceCourseId)
        val srcCourse = Courses.selectAll().where { Courses.id eq srcId }.singleOrNull()
            ?: throw IllegalArgumentException("Source course not found")

        val newCourseId = Courses.insertAndGetId {
            it[name] = newCourseName
            it[description] = srcCourse[Courses.description]
            it[authorId] = srcCourse[Courses.authorId].value
            it[authorUrl] = srcCourse[Courses.authorUrl]
            it[language] = newLanguage
            it[isPublished] = false
        }

        val srcThemes = Themes
            .selectAll().where { Themes.courseId eq srcId }
            .orderBy(Themes.createdAt to SortOrder.ASC)
            .toList()

        val oldToNew = mutableMapOf<UUID, EntityID<UUID>>()
        srcThemes.filter { it[Themes.parentThemeId] == null }.forEach { r ->
            val newId = Themes.insertAndGetId {
                it[courseId] = newCourseId
                it[parentThemeId] = null
                it[name] = r[Themes.name]
                it[description] = r[Themes.description]
                it[position] = r[Themes.position]
            }
            oldToNew[r[Themes.id].value] = newId
        }
        var pending = srcThemes.filter { it[Themes.parentThemeId] != null }
        while (pending.isNotEmpty()) {
            val next = mutableListOf<ResultRow>()
            for (r in pending) {
                val parentOld = r[Themes.parentThemeId]!!.value
                val parentNew = oldToNew[parentOld]
                if (parentNew != null) {
                    val newId = Themes.insertAndGetId {
                        it[courseId] = newCourseId
                        it[parentThemeId] = parentNew
                        it[name] = r[Themes.name]
                        it[description] = r[Themes.description]
                        it[position] = r[Themes.position]
                    }
                    oldToNew[r[Themes.id].value] = newId
                } else {
                    next.add(r)
                }
            }
            if (next.size == pending.size) break
            pending = next
        }

        Courses.selectAll().where { Courses.id eq newCourseId }.single().toCourse()
    }
}