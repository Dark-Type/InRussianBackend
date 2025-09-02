package com.inRussian.repositories.v2


import com.inRussian.tables.Sections
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.div
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.and
import kotlin.compareTo
import kotlin.div
import kotlin.text.set
import kotlin.text.toInt
import kotlin.times

class ProgressRepository() {

    val themeIdCol = Themes.id
    val themeSectionIdCol = Themes.sectionId

    val sectionIdCol = Sections.id
    val sectionCourseIdCol = Sections.courseId

    val taskIdCol = TaskEntity.id
    val taskThemeIdCol = TaskEntity.themeId

    suspend fun getSectionProgress(userId: UUID, sectionId: UUID): ResultRow? = newSuspendedTransaction(Dispatchers.IO) {
        UserSectionProgressTable
            .selectAll()
            .where { (UserSectionProgressTable.userId eq userId) and (UserSectionProgressTable.sectionId eq sectionId) }
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

    suspend fun computeSectionTotalTasks(sectionId: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        TaskEntity.join(Themes, JoinType.INNER, taskThemeIdCol, themeIdCol)
            .selectAll().where { themeSectionIdCol eq sectionId }
            .count().toInt()
    }

    suspend fun computeCourseTotalTasks(courseId: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        TaskEntity.join(Themes, JoinType.INNER, taskThemeIdCol, themeIdCol)
            .join(Sections, JoinType.INNER, themeSectionIdCol, sectionIdCol)
            .selectAll().where { sectionCourseIdCol eq courseId }
            .count().toInt()
    }

    // Upserts (first-try solve event)

    suspend fun applyFirstTrySolveToSection(
        userId: UUID,
        sectionId: UUID,
        courseId: UUID,
        firstTryTimeMs: Long,
        eventTime: Instant = Instant.now()
    ) = newSuspendedTransaction(Dispatchers.IO) {
        val total = computeSectionTotalTasks(sectionId)
        val inserted = UserSectionProgressTable.insertIgnore {
            it[UserSectionProgressTable.userId] = userId
            it[UserSectionProgressTable.sectionId] = sectionId
            it[UserSectionProgressTable.courseId] = courseId
            it[solvedTasks] = 1
            it[totalTasks] = total
            it[totalTimeMs] = firstTryTimeMs
            it[averageTimeMs] = firstTryTimeMs.toInt()
            it[percentComplete] = if (total > 0) (100.0 / total) else 0.0
            it[updatedAt] = Instant.now()
        }.insertedCount

        if (inserted == 0) {
            val row = UserSectionProgressTable
                .selectAll()
                .where { (UserSectionProgressTable.userId eq userId) and (UserSectionProgressTable.sectionId eq sectionId) }
                .limit(1)
                .firstOrNull()

            if (row != null) {
                val prevSolved = row[UserSectionProgressTable.solvedTasks]
                val prevTotalTime = row[UserSectionProgressTable.totalTimeMs]
                val solved = prevSolved + 1
                val newTotalTime = prevTotalTime + firstTryTimeMs
                val averageTime = if (solved > 0) (newTotalTime / solved).toInt() else 0
                val percent = if (total > 0) (solved * 100.0 / total) else 0.0

                UserSectionProgressTable.update(
                    where = { (UserSectionProgressTable.userId eq userId) and (UserSectionProgressTable.sectionId eq sectionId) }
                ) {
                    it[solvedTasks] = solved
                    it[totalTasks] = total
                    it[totalTimeMs] = newTotalTime
                    it[averageTimeMs] = averageTime
                    it[percentComplete] = percent
                    it[updatedAt] = Instant.now()
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
            it[updatedAt] = Instant.now()
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
                    it[updatedAt] = Instant.now()
                }
            }
        }
    }
}