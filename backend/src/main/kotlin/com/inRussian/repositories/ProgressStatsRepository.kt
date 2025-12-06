package com.inRussian.repositories

import com.inRussian.config.dbQuery
import com.inRussian.models.v2.AverageProgressDTO
import com.inRussian.models.v2.ThemeAverageDTO
import com.inRussian.tables.Courses
import com.inRussian.tables.UserCourseEnrollments
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.*
import java.time.ZoneOffset
import java.util.UUID


interface ProgressStatsRepository {
    suspend fun listEnrolledCourseIds(userId: UUID): List<UUID>
    suspend fun listCourseProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow>
    suspend fun listThemeProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow>
    suspend fun getCourseAverageProgress(courseId: UUID): AverageProgressDTO
    suspend fun getThemeAverageProgressByCourse(courseId: UUID): List<ThemeAverageDTO>
    suspend fun getPlatformCourseAverages(): AverageProgressDTO
    suspend fun getPlatformThemeAverages(): AverageProgressDTO
    suspend fun countTotalCourses(): Int
    suspend fun countTotalUsersWithProgress(): Int
}

class ExposedProgressStatsRepository : ProgressStatsRepository {

    override suspend fun listEnrolledCourseIds(userId: UUID): List<UUID> = dbQuery {
        UserCourseEnrollments
            .selectAll()
            .where { UserCourseEnrollments.userId eq userId }
            .map { it[UserCourseEnrollments.courseId].value }
    }

    override suspend fun listCourseProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> = dbQuery {
        if (courseIds.isEmpty()) emptyList()
        else UserCourseProgressTable
            .selectAll()
            .where { (UserCourseProgressTable.userId eq userId) and (UserCourseProgressTable.courseId inList courseIds) }
            .toList()
    }

    override suspend fun listThemeProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> = dbQuery {
        if (courseIds.isEmpty()) emptyList()
        else UserThemeProgressTable
            .selectAll()
            .where { (UserThemeProgressTable.userId eq userId) and (UserThemeProgressTable.courseId inList courseIds) }
            .orderBy(UserThemeProgressTable.themeId to SortOrder.ASC)
            .toList()
    }

    override suspend fun getCourseAverageProgress(courseId: UUID): AverageProgressDTO = dbQuery {
        val avgSolved = UserCourseProgressTable.solvedTasks.avg()
        val avgTotal = UserCourseProgressTable.totalTasks.avg()
        val avgPercent = UserCourseProgressTable.percentComplete.avg()
        val avgAvgTime = UserCourseProgressTable.averageTimeMs.avg()
        val participantsExpr = UserCourseProgressTable.userId.count()
        val maxUpdated = UserCourseProgressTable.updatedAt.max()

        val row = UserCourseProgressTable
            .select(avgSolved, avgTotal, avgPercent, avgAvgTime, participantsExpr, maxUpdated)
            .where { UserCourseProgressTable.courseId eq courseId }
            .firstOrNull()

        val participants = row?.get(participantsExpr) ?: 0L
        if (row == null || participants == 0L) {
            AverageProgressDTO(0.0, 0.0, 0.0, 0.0, 0, null)
        } else {
            AverageProgressDTO(
                solvedTasksAvg = row[avgSolved]?.toDouble() ?: 0.0,
                totalTasksAvg = row[avgTotal]?.toDouble() ?: 0.0,
                percentAvg = row[avgPercent]?.toDouble() ?: 0.0,
                averageTimeMsAvg = row[avgAvgTime]?.toDouble() ?: 0.0,
                participants = participants.toInt(),
                lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
            )
        }
    }

    override suspend fun getThemeAverageProgressByCourse(courseId: UUID): List<ThemeAverageDTO> = dbQuery {
        val avgSolved = UserThemeProgressTable.solvedTasks.avg()
        val avgTotal = UserThemeProgressTable.totalTasks.avg()
        val avgPercent = UserThemeProgressTable.percentComplete.avg()
        val avgAvgTime = UserThemeProgressTable.averageTimeMs.avg()
        val participantsExpr = UserThemeProgressTable.userId.count()
        val maxUpdated = UserThemeProgressTable.updatedAt.max()

        UserThemeProgressTable
            .select(
                UserThemeProgressTable.themeId,
                UserThemeProgressTable.courseId,
                avgSolved, avgTotal, avgPercent, avgAvgTime,
                participantsExpr, maxUpdated
            )
            .where { UserThemeProgressTable.courseId eq courseId }
            .groupBy(UserThemeProgressTable.themeId, UserThemeProgressTable.courseId)
            .map { row ->
                ThemeAverageDTO(
                    themeId = row[UserThemeProgressTable.themeId],
                    courseId = row[UserThemeProgressTable.courseId],
                    solvedTasksAvg = row[avgSolved]?.toDouble() ?: 0.0,
                    totalTasksAvg = row[avgTotal]?.toDouble() ?: 0.0,
                    percentAvg = row[avgPercent]?.toDouble() ?: 0.0,
                    averageTimeMsAvg = row[avgAvgTime]?.toDouble() ?: 0.0,
                    participants = row[participantsExpr].toInt(),
                    lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                )
            }
    }

    override suspend fun getPlatformCourseAverages(): AverageProgressDTO = dbQuery {
        val avgSolved = UserCourseProgressTable.solvedTasks.avg()
        val avgTotal = UserCourseProgressTable.totalTasks.avg()
        val avgPercent = UserCourseProgressTable.percentComplete.avg()
        val avgAvgTime = UserCourseProgressTable.averageTimeMs.avg()
        val participantsExpr = UserCourseProgressTable.userId.countDistinct()
        val maxUpdated = UserCourseProgressTable.updatedAt.max()

        val row = UserCourseProgressTable
            .select(avgSolved, avgTotal, avgPercent, avgAvgTime, participantsExpr, maxUpdated)
            .firstOrNull()

        val participants = row?.get(participantsExpr) ?: 0L
        if (row == null || participants == 0L) {
            AverageProgressDTO(0.0, 0.0, 0.0, 0.0, 0, null)
        } else {
            AverageProgressDTO(
                solvedTasksAvg = row[avgSolved]?.toDouble() ?: 0.0,
                totalTasksAvg = row[avgTotal]?.toDouble() ?: 0.0,
                percentAvg = row[avgPercent]?.toDouble() ?: 0.0,
                averageTimeMsAvg = row[avgAvgTime]?.toDouble() ?: 0.0,
                participants = participants.toInt(),
                lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
            )
        }
    }

    override suspend fun getPlatformThemeAverages(): AverageProgressDTO = dbQuery {
        val avgSolved = UserThemeProgressTable.solvedTasks.avg()
        val avgTotal = UserThemeProgressTable.totalTasks.avg()
        val avgPercent = UserThemeProgressTable.percentComplete.avg()
        val avgAvgTime = UserThemeProgressTable.averageTimeMs.avg()
        val participantsExpr = UserThemeProgressTable.userId.countDistinct()
        val maxUpdated = UserThemeProgressTable.updatedAt.max()

        val row = UserThemeProgressTable
            .select(avgSolved, avgTotal, avgPercent, avgAvgTime, participantsExpr, maxUpdated)
            .firstOrNull()

        val participants = row?.get(participantsExpr) ?: 0L
        if (row == null || participants == 0L) {
            AverageProgressDTO(0.0, 0.0, 0.0, 0.0, 0, null)
        } else {
            AverageProgressDTO(
                solvedTasksAvg = row[avgSolved]?.toDouble() ?: 0.0,
                totalTasksAvg = row[avgTotal]?.toDouble() ?: 0.0,
                percentAvg = row[avgPercent]?.toDouble() ?: 0.0,
                averageTimeMsAvg = row[avgAvgTime]?.toDouble() ?: 0.0,
                participants = participants.toInt(),
                lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
            )
        }
    }

    override suspend fun countTotalCourses(): Int = dbQuery {
        Courses.selectAll().count().toInt()
    }

    override suspend fun countTotalUsersWithProgress(): Int = dbQuery {
        val distinctUsers = UserCourseProgressTable.userId.countDistinct()
        UserCourseProgressTable
            .select(distinctUsers)
            .first()[distinctUsers]
            .toInt()
    }
}