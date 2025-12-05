package com.inRussian.repositories.v2

import com.inRussian.config.dbQuery
import com.inRussian.models.v2.AverageProgressDTO
import com.inRussian.models.v2.ThemeAverageDTO
import com.inRussian.tables.Courses
import com.inRussian.tables.UserCourseEnrollments
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.countDistinct
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import java.time.ZoneOffset
import java.util.UUID

class StatsRepository {

    suspend fun listEnrolledCourseIds(userId: UUID): List<UUID> = dbQuery {
        try {
            UserCourseEnrollments
                .selectAll().where { UserCourseEnrollments.userId eq userId }
                .map { it[UserCourseEnrollments.courseId].value }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun listCourseProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> = dbQuery {
        try {
            if (courseIds.isEmpty()) emptyList()
            else UserCourseProgressTable
                .selectAll().where {
                    (UserCourseProgressTable.userId eq userId) and
                            (UserCourseProgressTable.courseId inList courseIds)
                }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun listThemeProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> = dbQuery {
        try {
            if (courseIds.isEmpty()) emptyList()
            else UserThemeProgressTable
                .selectAll().where {
                    (UserThemeProgressTable.userId eq userId) and
                            (UserThemeProgressTable.courseId inList courseIds)
                }
                .orderBy(UserThemeProgressTable.themeId to SortOrder.ASC)
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCourseAverageProgress(courseId: UUID): AverageProgressDTO? = dbQuery {
        try {
            val avgSolved = UserCourseProgressTable.solvedTasks.avg()
            val avgTotal = UserCourseProgressTable.totalTasks.avg()
            val avgPercent = UserCourseProgressTable.percentComplete.avg()
            val avgAvgTime = UserCourseProgressTable.averageTimeMs.avg()
            val participantsExpr = UserCourseProgressTable.userId.count()
            val maxUpdated = UserCourseProgressTable.updatedAt.max()

            val row = UserCourseProgressTable
                .selectAll().where { UserCourseProgressTable.courseId eq courseId }
                .firstOrNull()

            val participants = row?.get(participantsExpr) ?: 0L
            if (row == null || participants == 0L) {
                AverageProgressDTO(
                    solvedTasksAvg = 0.0,
                    totalTasksAvg = 0.0,
                    percentAvg = 0.0,
                    averageTimeMsAvg = 0.0,
                    participants = 0,
                    lastUpdatedAt = null
                )
            } else {
                AverageProgressDTO(
                    solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
                    totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
                    percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
                    averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
                    participants = participants.toInt(),
                    lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                )
            }
        } catch (e: Exception) {
            AverageProgressDTO(
                solvedTasksAvg = 0.0,
                totalTasksAvg = 0.0,
                percentAvg = 0.0,
                averageTimeMsAvg = 0.0,
                participants = 0,
                lastUpdatedAt = null
            )
        }
    }

    suspend fun getThemeAverageProgressByCourse(courseId: UUID): List<ThemeAverageDTO> = dbQuery {
        try {
            val avgSolved = UserThemeProgressTable.solvedTasks.avg()
            val avgTotal = UserThemeProgressTable.totalTasks.avg()
            val avgPercent = UserThemeProgressTable.percentComplete.avg()
            val avgAvgTime = UserThemeProgressTable.averageTimeMs.avg()
            val participantsExpr = UserThemeProgressTable.userId.count()
            val maxUpdated = UserThemeProgressTable.updatedAt.max()

            UserThemeProgressTable
                .selectAll().where { UserThemeProgressTable.courseId eq courseId }
                .groupBy(UserThemeProgressTable.themeId, UserThemeProgressTable.courseId)
                .map { row ->
                    ThemeAverageDTO(
                        themeId = row[UserThemeProgressTable.themeId],
                        courseId = row[UserThemeProgressTable.courseId],
                        solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
                        totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
                        percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
                        averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
                        participants = row[participantsExpr].toInt(),
                        lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlatformCourseAverages(): AverageProgressDTO? = dbQuery {
        try {
            val avgSolved = UserCourseProgressTable.solvedTasks.avg()
            val avgTotal = UserCourseProgressTable.totalTasks.avg()
            val avgPercent = UserCourseProgressTable.percentComplete.avg()
            val avgAvgTime = UserCourseProgressTable.averageTimeMs.avg()
            val participantsExpr = UserCourseProgressTable.userId.countDistinct()
            val maxUpdated = UserCourseProgressTable.updatedAt.max()

            val row = UserCourseProgressTable
                .selectAll()
                .firstOrNull()

            val participants = row?.get(participantsExpr)?.toLong() ?: 0L
            if (row == null || participants == 0L) {
                AverageProgressDTO(
                    solvedTasksAvg = 0.0,
                    totalTasksAvg = 0.0,
                    percentAvg = 0.0,
                    averageTimeMsAvg = 0.0,
                    participants = 0,
                    lastUpdatedAt = null
                )
            } else {
                AverageProgressDTO(
                    solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
                    totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
                    percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
                    averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
                    participants = participants.toInt(),
                    lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                )
            }
        } catch (e: Exception) {
            AverageProgressDTO(
                solvedTasksAvg = 0.0,
                totalTasksAvg = 0.0,
                percentAvg = 0.0,
                averageTimeMsAvg = 0.0,
                participants = 0,
                lastUpdatedAt = null
            )
        }
    }

    suspend fun getPlatformThemeAverages(): AverageProgressDTO? = dbQuery {
        try {
            val avgSolved = UserThemeProgressTable.solvedTasks.avg()
            val avgTotal = UserThemeProgressTable.totalTasks.avg()
            val avgPercent = UserThemeProgressTable.percentComplete.avg()
            val avgAvgTime = UserThemeProgressTable.averageTimeMs.avg()
            val participantsExpr = UserThemeProgressTable.userId.countDistinct()
            val maxUpdated = UserThemeProgressTable.updatedAt.max()

            val row = UserThemeProgressTable
                .selectAll()
                .firstOrNull()

            val participants = row?.get(participantsExpr) ?: 0L
            if (row == null || participants == 0L) {
                AverageProgressDTO(
                    solvedTasksAvg = 0.0,
                    totalTasksAvg = 0.0,
                    percentAvg = 0.0,
                    averageTimeMsAvg = 0.0,
                    participants = 0,
                    lastUpdatedAt = null
                )
            } else {
                AverageProgressDTO(
                    solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
                    totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
                    percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
                    averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
                    participants = participants.toInt(),
                    lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                )
            }
        } catch (e: Exception) {
            AverageProgressDTO(
                solvedTasksAvg = 0.0,
                totalTasksAvg = 0.0,
                percentAvg = 0.0,
                averageTimeMsAvg = 0.0,
                participants = 0,
                lastUpdatedAt = null
            )
        }
    }

    suspend fun countTotalCourses(): Int = dbQuery {
        try {
            Courses.selectAll().count().toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun countTotalUsersWithProgress(): Int = dbQuery {
        try {
            UserCourseProgressTable
                .selectAll()
                .distinctBy { it[UserCourseProgressTable.userId] }
                .count()
        } catch (e: Exception) {
            0
        }
    }
}