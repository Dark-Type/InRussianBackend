package com.inRussian.repositories.v2

import com.inRussian.models.v2.AverageProgressDTO
import com.inRussian.models.v2.SectionAverageDTO
import com.inRussian.tables.Courses
import com.inRussian.tables.UserCourseEnrollments
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
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

    fun listEnrolledCourseIds(userId: UUID): List<UUID> =
        UserCourseEnrollments
            .selectAll().where { UserCourseEnrollments.userId eq userId }
            .map { it[UserCourseEnrollments.courseId].value }

    fun listCourseProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> =
        if (courseIds.isEmpty()) emptyList()
        else UserCourseProgressTable
            .selectAll().where {
                (UserCourseProgressTable.userId eq userId) and
                        (UserCourseProgressTable.courseId inList courseIds)
            }
            .toList()

    fun listSectionProgressForUser(userId: UUID, courseIds: List<UUID>): List<ResultRow> =
        if (courseIds.isEmpty()) emptyList()
        else UserSectionProgressTable
            .selectAll().where {
                (UserSectionProgressTable.userId eq userId) and
                        (UserSectionProgressTable.courseId inList courseIds)
            }
            .orderBy(UserSectionProgressTable.sectionId to SortOrder.ASC)
            .toList()

    fun getCourseAverageProgress(courseId: UUID): AverageProgressDTO? {
        val avgSolved = UserCourseProgressTable.solvedTasks.avg()
        val avgTotal = UserCourseProgressTable.totalTasks.avg()
        val avgPercent = UserCourseProgressTable.percentComplete.avg()
        val avgAvgTime = UserCourseProgressTable.averageTimeMs.avg()
        val participantsExpr = UserCourseProgressTable.userId.count()
        val maxUpdated = UserCourseProgressTable.updatedAt.max()

        val row = UserCourseProgressTable
            .selectAll().where { UserCourseProgressTable.courseId eq courseId }
            .firstOrNull()

        val participants = row?.get(participantsExpr)?.toLong() ?: 0L
        if (row == null || participants == 0L) return null

        return AverageProgressDTO(
            solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
            totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
            percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
            averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
            participants = participants.toInt(),
            lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
        )
    }

    fun getSectionAverageProgressByCourse(courseId: UUID): List<SectionAverageDTO> {
        val avgSolved = UserSectionProgressTable.solvedTasks.avg()
        val avgTotal = UserSectionProgressTable.totalTasks.avg()
        val avgPercent = UserSectionProgressTable.percentComplete.avg()
        val avgAvgTime = UserSectionProgressTable.averageTimeMs.avg()
        val participantsExpr = UserSectionProgressTable.userId.count()
        val maxUpdated = UserSectionProgressTable.updatedAt.max()

        return UserSectionProgressTable
            .selectAll().where { UserSectionProgressTable.courseId eq courseId }
            .groupBy(UserSectionProgressTable.sectionId, UserSectionProgressTable.courseId)
            .map { row ->
                SectionAverageDTO(
                    sectionId = row[UserSectionProgressTable.sectionId],
                    courseId = row[UserSectionProgressTable.courseId],
                    solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
                    totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
                    percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
                    averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
                    participants = row[participantsExpr].toLong().toInt(),
                    lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
                )
            }
    }

    fun getPlatformCourseAverages(): AverageProgressDTO? {
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
        if (row == null || participants == 0L) return null

        return AverageProgressDTO(
            solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
            totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
            percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
            averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
            participants = participants.toInt(),
            lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
        )
    }

    fun getPlatformSectionAverages(): AverageProgressDTO? {
        val avgSolved = UserSectionProgressTable.solvedTasks.avg()
        val avgTotal = UserSectionProgressTable.totalTasks.avg()
        val avgPercent = UserSectionProgressTable.percentComplete.avg()
        val avgAvgTime = UserSectionProgressTable.averageTimeMs.avg()
        val participantsExpr = UserSectionProgressTable.userId.countDistinct()
        val maxUpdated = UserSectionProgressTable.updatedAt.max()

        val row = UserSectionProgressTable
            .selectAll()
            .firstOrNull()

        val participants = row?.get(participantsExpr)?.toLong() ?: 0L
        if (row == null || participants == 0L) return null

        return AverageProgressDTO(
            solvedTasksAvg = (row[avgSolved] as? Number)?.toDouble() ?: 0.0,
            totalTasksAvg = (row[avgTotal] as? Number)?.toDouble() ?: 0.0,
            percentAvg = (row[avgPercent] as? Number)?.toDouble() ?: 0.0,
            averageTimeMsAvg = (row[avgAvgTime] as? Number)?.toDouble() ?: 0.0,
            participants = participants.toInt(),
            lastUpdatedAt = row[maxUpdated]?.atOffset(ZoneOffset.UTC)?.toInstant()
        )
    }

    fun countTotalCourses(): Int =
        Courses.selectAll().count().toInt()

    fun countTotalUsersWithProgress(): Int =
        UserCourseProgressTable
            .selectAll()
            .distinctBy { it[UserCourseProgressTable.userId] }
            .count()
}