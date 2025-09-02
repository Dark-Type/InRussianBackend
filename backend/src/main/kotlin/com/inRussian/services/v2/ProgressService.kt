package com.inRussian.services.v2

import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.SectionProgressDTO
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.tables.v2.UserCourseProgressTable
import com.inRussian.tables.v2.UserSectionProgressTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneOffset
import java.util.UUID

class ProgressService(
    private val progressRepo: ProgressRepository
) {

    suspend fun sectionProgress(userId: UUID, sectionId: UUID): SectionProgressDTO? =
        newSuspendedTransaction(Dispatchers.IO) {
            progressRepo.getSectionProgress(userId, sectionId)?.let(::toSectionDTO)
        }

    suspend fun courseProgress(userId: UUID, courseId: UUID): CourseProgressDTO? =
        newSuspendedTransaction(Dispatchers.IO) {
            progressRepo.getCourseProgress(userId, courseId)?.let(::toCourseDTO)
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