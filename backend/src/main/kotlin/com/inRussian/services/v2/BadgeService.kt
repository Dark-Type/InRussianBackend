package com.inRussian.services.v2

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.models.v2.AwardedBadgeDTO
import com.inRussian.repositories.BadgeRepository
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.collections.filter
import kotlin.compareTo
import kotlin.text.get

class BadgeService(
    private val badgeRepo: BadgeRepository
) {

    suspend fun handleThemeCompleted(userId: UUID, themeId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.THEME_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.themeId] == themeId }
            matched.mapNotNull { r ->
                val ok =
                    badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], courseId = courseId, themeId = themeId)
                if (ok) AwardedBadgeDTO(
                    badgeId = r[BadgeRuleTable.badgeId],
                    themeId = themeId,
                    courseId = courseId,
                    awardedAt = Instant.now()
                ) else null
            }
        }

    suspend fun handleCourseCompleted(userId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.COURSE_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.courseId] == courseId }
            matched.mapNotNull { r ->
                val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], courseId = courseId)
                if (ok) AwardedBadgeDTO(
                    badgeId = r[BadgeRuleTable.badgeId],
                    courseId = courseId,
                    awardedAt = Instant.now()
                ) else null
            }
        }

    suspend fun handleDailyStreak(userId: UUID): List<AwardedBadgeDTO> =
        dbQuery {
            val today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()

            badgeRepo.recordDailySolve(
                userId,
                dayUtc = today
            )

            val streak = badgeRepo.currentDailyStreak(
                userId,
                todayUtc = today
            )

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