package com.inRussian.services.v2

import com.inRussian.models.v2.AttemptInsert
import com.inRussian.models.v2.AwardedBadgeDTO
import com.inRussian.models.v2.CourseProgressDTO
import com.inRussian.models.v2.SolveResult
import com.inRussian.models.v2.ThemeProgressDTO
import com.inRussian.repositories.v2.AttemptRepository
import com.inRussian.repositories.v2.ProgressRepository
import com.inRussian.repositories.v2.QueueRepository
import com.inRussian.repositories.v2.TaskStateRepository
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
    private val badgeService: BadgeService
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

            val courseId = Themes
                .selectAll().where { Themes.id eq themeId }
                .limit(1)
                .first()[Themes.courseId].value

            queueRepo.ensureQueueState(params.userId, themeId)
            stateRepo.ensureStateRow(params.userId, params.taskId, themeId, courseId)

            attemptRepo.insertAttemptIfNew(
                AttemptInsert(
                    attemptId = params.attemptId,
                    userId = params.userId,
                    taskId = params.taskId,
                    themeId = themeId,
                    courseId = courseId,
                    attemptsCount = params.attemptsCount,
                    timeSpentMs = params.timeSpentMs,
                    createdAt = null
                )
            )

            var removed = false
            var moved = false
            var themeDTO: ThemeProgressDTO? = null
            var courseDTO: CourseProgressDTO? = null
            val newlyAwarded = mutableListOf<AwardedBadgeDTO>()
            var themeCompletedNow = false

            if (params.attemptsCount == 1) {
                removed = queueRepo.removeFromQueue(params.userId, themeId, params.taskId) > 0

                val firstSolved = stateRepo.markSolvedFirstTryIfNot(params.userId, params.taskId)
                if (firstSolved) {
                    progressRepo.applyFirstTrySolveToTheme(params.userId, themeId, courseId, params.timeSpentMs)
                    progressRepo.applyFirstTrySolveToCourse(params.userId, courseId, params.timeSpentMs)

                    themeDTO = ProgressService(progressRepo).themeProgress(params.userId, themeId)
                    courseDTO = ProgressService(progressRepo).courseProgress(params.userId, courseId)

                    newlyAwarded += badgeService.handleDailyStreak(params.userId)

                    if (themeDTO.totalTasks > 0 && themeDTO.percent >= 100.0) {
                        themeCompletedNow = true
                        newlyAwarded += badgeService.handleThemeCompleted(params.userId, themeId, courseId)
                    }
                    if (courseDTO.totalTasks > 0 && courseDTO.percent >= 100.0) {
                        newlyAwarded += badgeService.handleCourseCompleted(params.userId, courseId)
                    }
                }
            } else {
                // Wrong answer path: requeue at the end inside the same theme queue
                moved = queueRepo.moveToEnd(params.userId, themeId, params.taskId) > 0
            }

            val sizeAfter = queueRepo.getQueueSize(params.userId, themeId)
            if (sizeAfter == 1L || sizeAfter == 0L) {
                queueRepo.seedThemeTasks(params.userId, themeId)
            }

            SolveResult(
                removedFromQueue = removed,
                movedToEnd = moved,
                themeProgress = themeDTO,
                courseProgress = courseDTO,
                newlyAwardedBadges = newlyAwarded,
                themeCompleted = themeCompletedNow
            )
        }
}