package com.inRussian.services.v2

import com.inRussian.models.v2.CourseAverageStatsDTO
import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.CourseStatsDTO
import com.inRussian.models.v2.PlatformStatsDTO
import com.inRussian.models.v2.ThemeProgressDTO
import com.inRussian.models.v2.UserStatsDTO
import com.inRussian.repositories.v2.StatsRepository
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class StatsService(
    private val statsRepo: StatsRepository,
) {

    suspend fun userStats(userId: UUID): UserStatsDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            val courseIds = statsRepo.listEnrolledCourseIds(userId)

            val courseProgressRows = statsRepo.listCourseProgressForUser(userId, courseIds)
            val themeProgressRows = statsRepo.listThemeProgressForUser(userId, courseIds)

            val courseProgressById = courseProgressRows.associateBy { it[UserCourseProgressTable.courseId] }
            val themesByCourse = themeProgressRows.groupBy { it[UserThemeProgressTable.courseId] }

            val courseStats = courseIds.map { cid ->
                val cpRow = courseProgressById[cid]
                val cpDto = cpRow?.let(::toCourseDTO)
                val themeDtos = (themesByCourse[cid] ?: emptyList()).map(::toThemeDTO)
                CourseStatsDTO(
                    courseId = cid,
                    courseProgress = cpDto,
                    themes = themeDtos
                )
            }

            UserStatsDTO(
                userId = userId,
                courses = courseStats
            )
        }

    suspend fun courseAverageStats(courseId: UUID): CourseAverageStatsDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            val courseAvg = statsRepo.getCourseAverageProgress(courseId)
            val themesAvg = statsRepo.getThemeAverageProgressByCourse(courseId)
            CourseAverageStatsDTO(
                courseId = courseId,
                courseAverage = courseAvg,
                themesAverage = themesAvg
            )
        }

    suspend fun platformStats(): PlatformStatsDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            val totalCourses = statsRepo.countTotalCourses()
            val totalUsers = statsRepo.countTotalUsersWithProgress()
            val courseLevelAvg = statsRepo.getPlatformCourseAverages()
            val themeLevelAvg = statsRepo.getPlatformThemeAverages()

            PlatformStatsDTO(
                totalCourses = totalCourses,
                totalUsersWithProgress = totalUsers,
                courseLevelAverage = courseLevelAvg,
                themeLevelAverage = themeLevelAvg,
                generatedAt = Instant.now(),
            )
        }

    private fun toThemeDTO(row: ResultRow): ThemeProgressDTO =
        ThemeProgressDTO(
            userId = row[UserThemeProgressTable.userId],
            themeId = row[UserThemeProgressTable.themeId],
            solvedTasks = row[UserThemeProgressTable.solvedTasks],
            totalTasks = row[UserThemeProgressTable.totalTasks],
            percent = row[UserThemeProgressTable.percentComplete],
            averageTimeMs = row[UserThemeProgressTable.averageTimeMs],
            updatedAt = row[UserThemeProgressTable.updatedAt].atOffset(ZoneOffset.UTC).toInstant()
        )

    private fun toCourseDTO(row: ResultRow): CourseProgressDTO =
        CourseProgressDTO(
            userId = row[UserCourseProgressTable.userId],
            courseId = row[UserCourseProgressTable.courseId],
            solvedTasks = row[UserCourseProgressTable.solvedTasks],
            totalTasks = row[UserCourseProgressTable.totalTasks],
            percent = row[UserCourseProgressTable.percentComplete],
            averageTimeMs = row[UserCourseProgressTable.averageTimeMs],
            updatedAt = row[UserCourseProgressTable.updatedAt].atOffset(ZoneOffset.UTC).toInstant()
        )
}