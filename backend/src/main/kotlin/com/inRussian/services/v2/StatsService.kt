package com.inRussian.services.v2

import com.inRussian.models.v2.CourseAverageStatsDTO
import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.CourseStatsDTO
import com.inRussian.models.v2.PlatformStatsDTO
import com.inRussian.models.v2.ThemeProgressDTO
import com.inRussian.models.v2.UserStatsDTO
import com.inRussian.repositories.ContentStatsRepository
import com.inRussian.repositories.ProgressStatsRepository
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class StatsService(
    private val progressRepo: ProgressStatsRepository,
    private val contentRepo: ContentStatsRepository
) {

    suspend fun userStats(userId: UUID): UserStatsDTO {
        val courseIds = progressRepo.listEnrolledCourseIds(userId)

        val courseProgressRows = progressRepo.listCourseProgressForUser(userId, courseIds)
        val themeProgressRows = progressRepo.listThemeProgressForUser(userId, courseIds)

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

        return UserStatsDTO(
            userId = userId,
            courses = courseStats
        )
    }

    suspend fun courseAverageStats(courseId: UUID): CourseAverageStatsDTO {
        val courseAvg = progressRepo.getCourseAverageProgress(courseId)
        val themesAvg = progressRepo.getThemeAverageProgressByCourse(courseId)
        return CourseAverageStatsDTO(
            courseId = courseId,
            courseAverage = courseAvg,
            themesAverage = themesAvg
        )
    }

    suspend fun platformStats(): PlatformStatsDTO {
        val totalCourses = progressRepo.countTotalCourses()
        val totalUsers = progressRepo.countTotalUsersWithProgress()
        val courseLevelAvg = progressRepo.getPlatformCourseAverages()
        val themeLevelAvg = progressRepo.getPlatformThemeAverages()

        return PlatformStatsDTO(
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