package com.inRussian.services.v2

import com.inRussian.models.v2.BadgeDTO
import com.inRussian.models.v2.BadgeRuleDTO
import com.inRussian.models.v2.UserAwardedBadgeDTO
import com.inRussian.repositories.v2.BadgeRepository
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.UserBadgeTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneOffset
import java.util.UUID

class BadgesQueryService {

    suspend fun listUserBadges(userId: UUID): List<UserAwardedBadgeDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            UserBadgeTable
                .join(BadgeRuleTable, JoinType.LEFT, onColumn = UserBadgeTable.badgeId, otherColumn = BadgeRuleTable.badgeId)
                .selectAll().where { UserBadgeTable.userId eq userId }
                .orderBy(UserBadgeTable.awardedAt, org.jetbrains.exposed.sql.SortOrder.DESC)
                .map { toUserAwardedBadgeDTO(it) }
        }

    suspend fun getBadgeRules(badgeId: UUID): List<BadgeRuleDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            BadgeRuleTable
                .selectAll().where { BadgeRuleTable.badgeId eq badgeId }
                .map { toBadgeRuleDTO(it) }
        }

    private fun toUserAwardedBadgeDTO(row: ResultRow): UserAwardedBadgeDTO {
        val rule = toBadgeRuleDTO(row)
        return UserAwardedBadgeDTO(
            id = row[UserBadgeTable.id].value,
            userId = row[UserBadgeTable.userId],
            badgeId = row[UserBadgeTable.badgeId],
            sectionId = row[UserBadgeTable.sectionId],
            courseId = row[UserBadgeTable.courseId],
            awardedAt = row[UserBadgeTable.awardedAt].atOffset(ZoneOffset.UTC).toInstant(),
            rule = rule
        )
    }

    private fun toBadgeRuleDTO(row: ResultRow): BadgeRuleDTO =
        BadgeRuleDTO(
            badgeId = row[BadgeRuleTable.badgeId],
            type = row[BadgeRuleTable.type].name,
            sectionId = row[BadgeRuleTable.sectionId],
            courseId = row[BadgeRuleTable.courseId],
            streakDays = row[BadgeRuleTable.streakDays],
            active = row[BadgeRuleTable.active]
        )
}