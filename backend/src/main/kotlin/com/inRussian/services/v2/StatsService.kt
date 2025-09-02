package com.inRussian.services.v2

import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.CourseStatsDTO
import com.inRussian.models.v2.SectionProgressDTO
import com.inRussian.models.v2.UserStatsDTO
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.repositories.v2.StatsRepository
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneOffset
import java.util.UUID

class StatsService(
    private val statsRepo: StatsRepository,
    private val progressRepo: ProgressRepository
) {

    suspend fun userStats(userId: UUID): UserStatsDTO =
        newSuspendedTransaction(Dispatchers.IO) {
            val courseIds = statsRepo.listEnrolledCourseIds(userId)

            val courseProgressRows = statsRepo.listCourseProgressForUser(userId, courseIds)
            val sectionProgressRows = statsRepo.listSectionProgressForUser(userId, courseIds)

            val courseProgressById = courseProgressRows.associateBy { it[UserCourseProgressTable.courseId] }
            val sectionsByCourse = sectionProgressRows.groupBy { it[UserSectionProgressTable.courseId] }

            val courseStats = courseIds.map { cid ->
                val cpRow = courseProgressById[cid]
                val cpDto = cpRow?.let(::toCourseDTO)
                val secDtos = (sectionsByCourse[cid] ?: emptyList()).map(::toSectionDTO)
                CourseStatsDTO(
                    courseId = cid,
                    courseProgress = cpDto,
                    sections = secDtos
                )
            }

            UserStatsDTO(
                userId = userId,
                courses = courseStats
            )
        }

    private fun toSectionDTO(row: ResultRow): SectionProgressDTO =
        SectionProgressDTO(
            userId = row[UserSectionProgressTable.userId],
            sectionId = row[UserSectionProgressTable.sectionId],
            courseId = row[UserSectionProgressTable.courseId],
            solvedTasks = row[UserSectionProgressTable.solvedTasks],
            totalTasks = row[UserSectionProgressTable.totalTasks],
            percent = row[UserSectionProgressTable.percentComplete],
            averageTimeMs = row[UserSectionProgressTable.averageTimeMs],
            updatedAt = row[UserSectionProgressTable.updatedAt].atOffset(ZoneOffset.UTC).toInstant()
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