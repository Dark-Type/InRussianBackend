package com.inRussian.services.v2

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.models.v2.AwardedBadgeDTO
import com.inRussian.repositories.v2.BadgeRepository
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID
import kotlin.collections.filter

class BadgeService(
    private val badgeRepo: BadgeRepository
) {

    // New: theme-completed handler (sections removed)
    suspend fun handleThemeCompleted(userId: UUID, themeId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.THEME_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.themeId] == themeId }
            matched.mapNotNull { r ->
                val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], courseId = courseId, themeId = themeId)
                if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId], themeId = themeId, courseId = courseId, awardedAt = Instant.now()) else null
            }
        }

    suspend fun handleCourseCompleted(userId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.COURSE_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.courseId] == courseId }
            matched.mapNotNull { r ->
                val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], courseId = courseId)
                if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId], courseId = courseId, awardedAt = Instant.now()) else null
            }
        }

    suspend fun handleDailyStreak(userId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            badgeRepo.recordDailySolve(userId)
            val streak = badgeRepo.currentDailyStreak(userId)
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.DAILY_STREAK)
            rules.mapNotNull { r ->
                val need = r[BadgeRuleTable.streakDays] ?: return@mapNotNull null
                if (streak >= need) {
                    val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId])
                    if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId], awardedAt = Instant.now()) else null
                } else null
            }
        }
}