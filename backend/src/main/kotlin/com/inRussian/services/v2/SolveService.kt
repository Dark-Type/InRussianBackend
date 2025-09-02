package com.inRussian.services.v2

import com.inRussian.models.v2.AttemptInsert
import com.inRussian.models.v2.AwardedBadgeDTO
import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.SectionProgressDTO
import com.inRussian.models.v2.SolveResult
import com.inRussian.repositories.v2.AttemptRepository
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.repositories.v2.QueueRepository
import com.inRussian.repositories.v2.SectionCompletionRepository
import com.inRussian.repositories.v2.TaskStateRepository
import com.inRussian.tables.Sections
import com.inRussian.tables.TaskEntity
import com.inRussian.tables.Themes
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

class SolveService(
    private val attemptRepo: AttemptRepository,
    private val stateRepo: TaskStateRepository,
    private val queueRepo: QueueRepository,
    private val progressRepo: ProgressRepository,
    private val badgeService: BadgeService,
    private val completionRepo: SectionCompletionRepository
) {
    @Serializable
    data class SubmitParams(
        @Contextual val attemptId: UUID,
        @Contextual val userId: UUID,
        @Contextual val taskId: UUID,
        val attemptsCount: Int,
        val timeSpentMs: Long
    )

    suspend fun submitSolved(params: SubmitParams): SolveResult =
        newSuspendedTransaction(Dispatchers.IO) {
            val themeId = TaskEntity
                .selectAll().where { TaskEntity.id eq params.taskId }
                .limit(1)
                .firstOrNull()?.get(TaskEntity.themeId)?.value
                ?: throw IllegalArgumentException("Task not found: ${params.taskId}")

            val sectionId = Themes
                .selectAll().where { Themes.id eq themeId }
                .limit(1)
                .first()[Themes.sectionId].value

            val courseId = Sections
                .selectAll().where { Sections.id eq sectionId }
                .limit(1)
                .first()[Sections.courseId].value

            queueRepo.ensureQueueState(params.userId, sectionId)
            stateRepo.ensureStateRow(params.userId, params.taskId, themeId, sectionId, courseId)

            attemptRepo.insertAttemptIfNew(
                AttemptInsert(
                    attemptId = params.attemptId,
                    userId = params.userId,
                    taskId = params.taskId,
                    themeId = themeId,
                    sectionId = sectionId,
                    courseId = courseId,
                    attemptsCount = params.attemptsCount,
                    timeSpentMs = params.timeSpentMs,
                    createdAt = null
                )
            )

            var removed = false
            var moved = false
            var sectionDTO: SectionProgressDTO? = null
            var courseDTO: CourseProgressDTO? = null
            val newlyAwarded = mutableListOf<AwardedBadgeDTO>()
            var sectionCompletedNow = false

            if (params.attemptsCount == 1) {
                removed = queueRepo.removeFromQueue(params.userId, sectionId, params.taskId) > 0

                val firstSolved = stateRepo.markSolvedFirstTryIfNot(params.userId, params.taskId)
                if (firstSolved) {
                    progressRepo.applyFirstTrySolveToSection(params.userId, sectionId, courseId, params.timeSpentMs)
                    progressRepo.applyFirstTrySolveToCourse(params.userId, courseId, params.timeSpentMs)

                    sectionDTO = ProgressService(progressRepo).sectionProgress(params.userId, sectionId)
                    courseDTO = ProgressService(progressRepo).courseProgress(params.userId, courseId)

                    newlyAwarded += badgeService.handleDailyStreak(params.userId)

                    if (sectionDTO != null && sectionDTO.totalTasks > 0 && sectionDTO.percent >= 100.0) {
                        newlyAwarded += badgeService.handleSectionCompleted(params.userId, sectionId, courseId)
                    }
                    if (courseDTO != null && courseDTO.totalTasks > 0 && courseDTO.percent >= 100.0) {
                        newlyAwarded += badgeService.handleCourseCompleted(params.userId, courseId)
                    }
                }
            } else {
                moved = queueRepo.moveToEnd(params.userId, sectionId, params.taskId) > 0
            }

            val sizeAfter = queueRepo.getQueueSize(params.userId, sectionId)
            if (sizeAfter == 1L) {
                queueRepo.seedNextTheme(params.userId, sectionId)
            } else if (sizeAfter == 0L) {
                val added = queueRepo.seedNextTheme(params.userId, sectionId)
                if (added == 0) {
                    completionRepo.markCompletedFlag(params.userId, sectionId, completed = true)
                    sectionCompletedNow = true
                }
            }

            SolveResult(
                removedFromQueue = removed,
                movedToEnd = moved,
                sectionProgress = sectionDTO,
                courseProgress = courseDTO,
                newlyAwardedBadges = newlyAwarded,
                sectionCompleted = sectionCompletedNow
            )
        }
}