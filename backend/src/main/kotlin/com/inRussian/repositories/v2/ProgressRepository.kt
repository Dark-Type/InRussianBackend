package com.inRussian.repositories.v2


import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.time.Instant

class ProgressRepository {

    private val themeIdCol = Themes.id
    private val themeCourseIdCol = Themes.courseId

    private val taskIdCol = TaskEntity.id
    private val taskThemeIdCol = TaskEntity.themeId

    suspend fun getThemeProgress(userId: UUID, themeId: UUID): ResultRow? = newSuspendedTransaction(Dispatchers.IO) {
        UserThemeProgressTable
            .selectAll()
            .where { (UserThemeProgressTable.userId eq userId) and (UserThemeProgressTable.themeId eq themeId) }
            .limit(1)
            .firstOrNull()
    }

    suspend fun getCourseProgress(userId: UUID, courseId: UUID): ResultRow? = newSuspendedTransaction(Dispatchers.IO) {
        UserCourseProgressTable
            .selectAll()
            .where { (UserCourseProgressTable.userId eq userId) and (UserCourseProgressTable.courseId eq courseId) }
            .limit(1)
            .firstOrNull()
    }

    // Totals

    suspend fun computeThemeTotalTasks(themeId: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        TaskEntity
            .selectAll().where { taskThemeIdCol eq themeId }
            .count().toInt()
    }

    suspend fun computeCourseTotalTasks(courseId: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        TaskEntity.join(Themes, JoinType.INNER, taskThemeIdCol, themeIdCol)
            .selectAll().where { themeCourseIdCol eq courseId }
            .count().toInt()
    }

    // Upserts (first-try solve event)

    suspend fun applyFirstTrySolveToTheme(
        userId: UUID,
        themeId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant = Instant.now()
    ) = newSuspendedTransaction(Dispatchers.IO) {
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

    suspend fun applyFirstTrySolveToCourse(
        userId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant = Instant.now()
    ) = newSuspendedTransaction(Dispatchers.IO) {
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