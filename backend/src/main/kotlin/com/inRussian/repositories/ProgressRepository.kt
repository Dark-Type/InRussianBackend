package com.inRussian.repositories

import com.inRussian.config.DatabaseFactory
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

interface ProgressRepository {
    suspend fun getThemeProgress(userId: UUID, themeId: UUID): ResultRow?
    suspend fun getCourseProgress(userId: UUID, courseId: UUID): ResultRow?

    suspend fun computeThemeTotalTasks(themeId: UUID): Int
    suspend fun computeCourseTotalTasks(courseId: UUID): Int

    suspend fun applyFirstTrySolveToTheme(
        userId: UUID,
        themeId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant = Instant.now()
    )

    suspend fun applyFirstTrySolveToCourse(
        userId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant = Instant.now()
    )
}

class ExposedProgressRepository : ProgressRepository {

    private val themeIdCol = Themes.id
    private val themeCourseIdCol = Themes.courseId

    private val taskThemeIdCol = TaskEntity.themeId

    override suspend fun getThemeProgress(userId: UUID, themeId: UUID): ResultRow? = DatabaseFactory.dbQuery {
        UserThemeProgressTable
            .selectAll()
            .where { (UserThemeProgressTable.userId eq userId) and (UserThemeProgressTable.themeId eq themeId) }
            .limit(1)
            .firstOrNull()
    }

    override suspend fun getCourseProgress(userId: UUID, courseId: UUID): ResultRow? = DatabaseFactory.dbQuery {
        UserCourseProgressTable
            .selectAll()
            .where { (UserCourseProgressTable.userId eq userId) and (UserCourseProgressTable.courseId eq courseId) }
            .limit(1)
            .firstOrNull()
    }


    override suspend fun computeThemeTotalTasks(themeId: UUID): Int = DatabaseFactory.dbQuery {
        TaskEntity
            .selectAll().where { taskThemeIdCol eq themeId }
            .count().toInt()
    }

    override suspend fun computeCourseTotalTasks(courseId: UUID): Int = DatabaseFactory.dbQuery {
        TaskEntity.join(Themes, JoinType.INNER, taskThemeIdCol, themeIdCol)
            .selectAll().where { themeCourseIdCol eq courseId }
            .count().toInt()
    }

    override suspend fun applyFirstTrySolveToTheme(
        userId: UUID,
        themeId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant
    ) = DatabaseFactory.dbQuery {
        val total = computeThemeTotalTasks(themeId)
        val inserted = UserThemeProgressTable.insertIgnore {
            it[UserThemeProgressTable.userId] = userId
            it[UserThemeProgressTable.themeId] = themeId
            it[UserThemeProgressTable.courseId] = courseId
            it[solvedTasks] = 1
            it[totalTasks] = total
            it[totalTimeMs] = firstTryTimeMs
            it[averageTimeMs] = firstTryTimeMs.toInt()
            it[percentComplete] = if (total > 0) (100.0 / total) else 0.0
            it[updatedAt] = eventTime
        }.insertedCount

        if (inserted == 0) {
            val row = UserThemeProgressTable
                .selectAll()
                .where { (UserThemeProgressTable.userId eq userId) and (UserThemeProgressTable.themeId eq themeId) }
                .limit(1)
                .firstOrNull()

            if (row != null) {
                val prevSolved = row[UserThemeProgressTable.solvedTasks]
                val prevTotalTime = row[UserThemeProgressTable.totalTimeMs]
                val solved = prevSolved + 1
                val newTotalTime = prevTotalTime + firstTryTimeMs
                val averageTime = if (solved > 0) (newTotalTime / solved).toInt() else 0
                val percent = if (total > 0) (solved * 100.0 / total) else 0.0

                UserThemeProgressTable.update(
                    where = { (UserThemeProgressTable.userId eq userId) and (UserThemeProgressTable.themeId eq themeId) }
                ) {
                    it[solvedTasks] = solved
                    it[totalTasks] = total
                    it[totalTimeMs] = newTotalTime
                    it[averageTimeMs] = averageTime
                    it[percentComplete] = percent
                    it[updatedAt] = eventTime
                }
            }
        }
    }

    override suspend fun applyFirstTrySolveToCourse(
        userId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant
    ) = DatabaseFactory.dbQuery {
        val total = computeCourseTotalTasks(courseId)
        val inserted = UserCourseProgressTable.insertIgnore {
            it[UserCourseProgressTable.userId] = userId
            it[UserCourseProgressTable.courseId] = courseId
            it[solvedTasks] = 1
            it[totalTasks] = total
            it[totalTimeMs] = firstTryTimeMs
            it[averageTimeMs] = firstTryTimeMs.toInt()
            it[percentComplete] = if (total > 0) (100.0 / total) else 0.0
            it[updatedAt] = eventTime
        }.insertedCount

        if (inserted == 0) {
            val row = UserCourseProgressTable
                .selectAll()
                .where { (UserCourseProgressTable.userId eq userId) and (UserCourseProgressTable.courseId eq courseId) }
                .limit(1)
                .firstOrNull()

            if (row != null) {
                val prevSolved = row[UserCourseProgressTable.solvedTasks]
                val prevTotalTime = row[UserCourseProgressTable.totalTimeMs]
                val solved = prevSolved + 1
                val newTotalTime = prevTotalTime + firstTryTimeMs
                val averageTime = if (solved > 0) (newTotalTime / solved).toInt() else 0
                val percent = if (total > 0) (solved * 100.0 / total) else 0.0

                UserCourseProgressTable.update(
                    where = { (UserCourseProgressTable.userId eq userId) and (UserCourseProgressTable.courseId eq courseId) }
                ) {
                    it[solvedTasks] = solved
                    it[totalTasks] = total
                    it[totalTimeMs] = newTotalTime
                    it[averageTimeMs] = averageTime
                    it[percentComplete] = percent
                    it[updatedAt] = eventTime
                }
            }
        }
    }
}