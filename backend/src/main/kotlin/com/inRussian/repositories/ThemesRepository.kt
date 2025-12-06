package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.content.Theme
import com.inRussian.models.tasks.TaskModel

import com.inRussian.requests.content.CreateThemeRequest
import com.inRussian.requests.content.UpdateThemeRequest
import com.inRussian.tables.Courses
import com.inRussian.tables.Themes
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.ArrayDeque
import java.util.UUID

@kotlinx.serialization.Serializable
data class ThemeContents(
    val theme: Theme,
    val childThemes: List<Theme>,
    val tasks: List<TaskModel>
)

@kotlinx.serialization.Serializable
data class ThemeTreeNode(
    val theme: Theme,
    val children: List<ThemeTreeNode>
)

interface ThemesRepository {
    suspend fun create(req: CreateThemeRequest): Theme
    suspend fun findById(id: String): Theme?
    suspend fun update(id: String, req: UpdateThemeRequest): Theme?
    suspend fun deleteCascade(id: String): Boolean
    suspend fun listRootByCourse(courseId: String): List<Theme>
    suspend fun listChildren(parentId: String): List<Theme>
    suspend fun getContents(themeId: String): ThemeContents?
    suspend fun getSubtree(themeId: String): ThemeTreeNode?
    suspend fun getCourseTree(courseId: String): List<ThemeTreeNode>
    suspend fun collectSubtreeIds(root: UUID): List<UUID>
}

class ExposedThemesRepository(
    private val tasks: TasksRepository
) : ThemesRepository {

    override suspend fun create(req: CreateThemeRequest): Theme = dbQuery {
        val parentId = req.parentThemeId?.let(UUID::fromString)
        val courseUuid = req.courseId?.let(UUID::fromString) ?: throw IllegalArgumentException("courseId required")
        val pos = ((req.position ?: (Themes.selectAll()
            .where { (Themes.courseId eq courseUuid) and (Themes.parentThemeId eq parentId) }
            .count() + 1))).toInt()

        val newId = Themes.insertAndGetId {
            it[name] = req.name
            it[description] = req.description
            it[courseId] = EntityID(courseUuid, Courses)
            it[parentThemeId] = parentId?.let { pid -> EntityID(pid, Themes) }
            it[position] = pos
        }
        Themes.selectAll().where { Themes.id eq newId }.single().toTheme()
    }

    override suspend fun findById(id: String): Theme? = dbQuery {
        Themes.selectAll().where { Themes.id eq UUID.fromString(id) }.singleOrNull()?.toTheme()
    }

    override suspend fun update(id: String, req: UpdateThemeRequest): Theme? = dbQuery {
        val uuid = UUID.fromString(id)
        req.parentThemeId?.let { pid ->
            val newParent = UUID.fromString(pid)
            if (tasks.themeHasTasksTx(newParent)) { // tx-local check
                throw IllegalStateException("Cannot set parent theme that already has tasks")
            }
        }
        Themes.update({ Themes.id eq uuid }) { row ->
            req.name?.let { row[Themes.name] = it }
            req.description?.let { row[Themes.description] = it }
            req.position?.let { row[Themes.position] = it }
            req.parentThemeId?.let { pid -> row[Themes.parentThemeId] = EntityID(UUID.fromString(pid), Themes) }
        }
        Themes.selectAll().where { Themes.id eq uuid }.singleOrNull()?.toTheme()
    }

    override suspend fun deleteCascade(id: String): Boolean = dbQuery {
        val root = UUID.fromString(id)
        val subtreeIds = collectSubtreeIdsTx(root)
        tasks.deleteByThemesTx(subtreeIds) // tx-local delete
        Themes.deleteWhere { Themes.id inList subtreeIds } > 0
    }

    override suspend fun listRootByCourse(courseId: String): List<Theme> = dbQuery {
        Themes.selectAll()
            .where { (Themes.courseId eq UUID.fromString(courseId)) and Themes.parentThemeId.isNull() }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun listChildren(parentId: String): List<Theme> = dbQuery {
        Themes.selectAll()
            .where { Themes.parentThemeId eq UUID.fromString(parentId) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
    }

    override suspend fun getContents(themeId: String): ThemeContents? = dbQuery {
        val themeRow = Themes.selectAll().where { Themes.id eq UUID.fromString(themeId) }.singleOrNull()
            ?: return@dbQuery null
        val theme = themeRow.toTheme()
        val children = Themes.selectAll()
            .where { Themes.parentThemeId eq UUID.fromString(theme.id) }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }
        val tasksList = tasks.listByThemeTx(UUID.fromString(theme.id)) // tx-local, no suspend
        ThemeContents(theme = theme, childThemes = children, tasks = tasksList)
    }

    override suspend fun getSubtree(themeId: String): ThemeTreeNode? = dbQuery {
        val root = Themes.selectAll().where { Themes.id eq UUID.fromString(themeId) }.singleOrNull()?.toTheme()
            ?: return@dbQuery null
        fun buildNode(current: Theme): ThemeTreeNode {
            val children = Themes.selectAll()
                .where { Themes.parentThemeId eq UUID.fromString(current.id) }
                .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
                .map { it.toTheme() }
            return ThemeTreeNode(theme = current, children = children.map { buildNode(it) })
        }
        buildNode(root)
    }

    override suspend fun getCourseTree(courseId: String): List<ThemeTreeNode> = dbQuery {
        val roots = Themes.selectAll()
            .where { (Themes.courseId eq UUID.fromString(courseId)) and Themes.parentThemeId.isNull() }
            .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
            .map { it.toTheme() }

        fun buildNode(current: Theme): ThemeTreeNode {
            val children = Themes.selectAll()
                .where { Themes.parentThemeId eq UUID.fromString(current.id) }
                .orderBy(Themes.position to SortOrder.ASC, Themes.createdAt to SortOrder.ASC)
                .map { it.toTheme() }
            return ThemeTreeNode(theme = current, children = children.map { buildNode(it) })
        }

        roots.map { buildNode(it) }
    }

    override suspend fun collectSubtreeIds(root: UUID): List<UUID> = dbQuery {
        collectSubtreeIdsTx(root)
    }

    private fun collectSubtreeIdsTx(root: UUID): List<UUID> {
        val result = LinkedHashSet<UUID>()
        val queue = ArrayDeque<UUID>()
        result.add(root); queue.add(root)
        while (queue.isNotEmpty()) {
            val batch = queue.toList()
            queue.clear()
            Themes.selectAll().where { Themes.parentThemeId inList batch }.forEach { row ->
                val child = row[Themes.id].value
                if (result.add(child)) queue.add(child)
            }
        }
        return result.toList()
    }

    private fun ResultRow.toTheme() = Theme(
        id = this[Themes.id].toString(),
        courseId = this[Themes.courseId].toString(),
        parentThemeId = this[Themes.parentThemeId]?.toString(),
        name = this[Themes.name],
        description = this[Themes.description],
        position = this[Themes.position],
        createdAt = this[Themes.createdAt].toString()
    )
}