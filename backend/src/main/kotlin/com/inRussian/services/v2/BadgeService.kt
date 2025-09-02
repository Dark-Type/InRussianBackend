package com.inRussian.services.v2

import com.inRussian.models.v2.AwardedBadgeDTO
import com.inRussian.repositories.v2.BadgeRepository
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class BadgeService(
    private val badgeRepo: BadgeRepository
) {

    suspend fun handleSectionCompleted(userId: UUID, sectionId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.SECTION_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.sectionId] == sectionId }
            matched.mapNotNull { r ->
                val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], sectionId = sectionId, courseId = courseId)
                if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId], sectionId = sectionId, courseId = courseId) else null
            }
        }

    suspend fun handleCourseCompleted(userId: UUID, courseId: UUID): List<AwardedBadgeDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.COURSE_COMPLETED)
            val matched = rules.filter { r -> r[BadgeRuleTable.courseId] == courseId }
            matched.mapNotNull { r ->
                val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId], courseId = courseId)
                if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId], courseId = courseId) else null
            }
        }

    suspend fun handleDailyStreak(userId: UUID): List<AwardedBadgeDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            badgeRepo.recordDailySolve(userId)
            val streak = badgeRepo.currentDailyStreak(userId)
            val rules = badgeRepo.listActiveRulesByType(SimpleBadgeRuleType.DAILY_STREAK)
            rules.mapNotNull { r ->
                val need = r[BadgeRuleTable.streakDays] ?: return@mapNotNull null
                if (streak >= need) {
                    val ok = badgeRepo.awardIfAbsent(userId, r[BadgeRuleTable.badgeId])
                    if (ok) AwardedBadgeDTO(badgeId = r[BadgeRuleTable.badgeId]) else null
                } else null
            }
        }
}