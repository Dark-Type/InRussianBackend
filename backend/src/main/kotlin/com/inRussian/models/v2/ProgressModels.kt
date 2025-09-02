package com.inRussian.models.v2


import kotlinx.serialization.Contextual
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable


data class AttemptInsert(
    val attemptId: UUID,
    val userId: UUID,
    val taskId: UUID,
    val themeId: UUID,
    val sectionId: UUID,
    val courseId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long,
    val createdAt: Instant? = null
)

data class AttemptRecord(
    val id: UUID,
    val userId: UUID,
    val taskId: UUID,
    val attemptsCount: Int,
    val timeSpentMs: Long,
    val createdAt: Instant
)
@Serializable
data class SectionProgressDTO(
    @Contextual val userId: UUID,
    @Contextual val sectionId: UUID,
    @Contextual val courseId: UUID,
    val solvedTasks: Int,
    val totalTasks: Int,
    val percent: Double,
    val averageTimeMs: Int,
    @Contextual val updatedAt: Instant
)

@Serializable
data class CourseProgressDTO(
    @Contextual val userId: UUID,
    @Contextual val courseId: UUID,
    val solvedTasks: Int,
    val totalTasks: Int,
    val percent: Double,
    val averageTimeMs: Int,
    @Contextual val updatedAt: Instant
)

@Serializable
data class AwardedBadgeDTO(
    @Contextual val badgeId: UUID,
    @Contextual val sectionId: UUID? = null,
    @Contextual val courseId: UUID? = null
)

@Serializable
data class SolveResult(
    val removedFromQueue: Boolean,
    val movedToEnd: Boolean,
    val sectionProgress: SectionProgressDTO? = null,
    val courseProgress: CourseProgressDTO? = null,
    val newlyAwardedBadges: List<AwardedBadgeDTO> = emptyList(),
    val sectionCompleted: Boolean
)

@Serializable
data class NextTaskResult(
    @Contextual val taskId: UUID,
    @Contextual val sectionId: UUID,
    @Contextual val themeId: UUID
)
@Serializable
data class BadgeDTO(
    @Contextual val id: UUID,
    @Contextual val badgeId: UUID,
    @Contextual val sectionId: UUID? = null,
    @Contextual val courseId: UUID? = null,
    @Contextual val awardedAt: Instant
)
@Serializable
data class UserStatsDTO(
    @Contextual val userId: UUID,
    val courses: List<CourseStatsDTO>
)

@Serializable
data class CourseStatsDTO(
    @Contextual val courseId: UUID,
    val courseProgress: CourseProgressDTO?,
    val sections: List<SectionProgressDTO>
)
@Serializable
data class BadgeRuleDTO(
    @Contextual val badgeId: UUID,
    val type: String,
    @Contextual val sectionId: UUID? = null,
    @Contextual val courseId: UUID? = null,
    val streakDays: Int? = null,
    val active: Boolean
)


@Serializable
data class UserAwardedBadgeDTO(
    @Contextual val id: UUID,
    @Contextual val userId: UUID,
    @Contextual val badgeId: UUID,
    @Contextual val sectionId: UUID? = null,
    @Contextual val courseId: UUID? = null,
    @Contextual val awardedAt: Instant,
    val rule: BadgeRuleDTO
)