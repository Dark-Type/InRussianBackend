package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.content.CountStats
import com.inRussian.tables.Courses
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

interface ContentStatsRepository {
    suspend fun countStats(): CountStats
    suspend fun courseTasksCount(courseId: String): Long
    suspend fun themeTasksCount(themeId: String): Long
}

class ExposedContentStatsRepository(
    private val tasks: com.inRussian.repositories.TasksRepository,
    private val themes: com.inRussian.repositories.ThemesRepository,
) : com.inRussian.repositories.ContentStatsRepository {
    override suspend fun countStats(): CountStats = dbQuery {
        val coursesCount = Courses.selectAll().count()
        val themesCount = Themes.selectAll().count()
        val tasksCount = TaskEntity.selectAll().count()
        CountStats(coursesCount, themesCount, tasksCount)
    }

    override suspend fun courseTasksCount(courseId: String): Long =
        tasks.countByCourse(UUID.fromString(courseId))

    override suspend fun themeTasksCount(themeId: String): Long {
        val subtree = themes.collectSubtreeIds(UUID.fromString(themeId))
        return tasks.countByThemes(subtree)
    }
}