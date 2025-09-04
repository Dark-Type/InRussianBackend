package com.inRussian.services.v2

import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.ThemeProgressDTO
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserThemeProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ProgressService(
    private val progressRepo: ProgressRepository
) {

    suspend fun themeProgress(userId: UUID, themeId: UUID): ThemeProgressDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            progressRepo.getThemeProgress(userId, themeId)
                ?.let(::toThemeDTO)
                ?: ThemeProgressDTO(
                    userId = userId,
                    themeId = themeId,
                    solvedTasks = 0,
                    totalTasks = 0,
                    percent = 0.0,
                    averageTimeMs = 0,
                    updatedAt = Instant.now()
                )
        }

    suspend fun courseProgress(userId: UUID, courseId: UUID): CourseProgressDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            progressRepo.getCourseProgress(userId, courseId)
                ?.let(::toCourseDTO)
                ?: CourseProgressDTO(
                    userId = userId,
                    courseId = courseId,
                    solvedTasks = 0,
                    totalTasks = 0,
                    percent = 0.0,
                    averageTimeMs = 0,
                    updatedAt = Instant.now()
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