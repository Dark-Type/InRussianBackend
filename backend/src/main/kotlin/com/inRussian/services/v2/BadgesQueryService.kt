package com.inRussian.services.v2

import com.inRussian.models.v2.BadgeDTO
import com.inRussian.repositories.v2.BadgeRepository
import com.inRussian.tables.v2.UserBadgeTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.ZoneOffset
import java.util.UUID

class BadgesQueryService(
    private val badgeRepo: BadgeRepository
) {
    suspend fun listUserBadges(userId: UUID): List<BadgeDTO> =
        newSuspendedTransaction(Dispatchers.IO) {
            badgeRepo.listUserBadges(userId).map(::toDTO)
        }

    private fun toDTO(row: ResultRow): BadgeDTO =
        BadgeDTO(
            id = row[UserBadgeTable.id].value,
            badgeId = row[UserBadgeTable.badgeId],
            sectionId = row[UserBadgeTable.sectionId],
            courseId = row[UserBadgeTable.courseId],
            awardedAt = row[UserBadgeTable.awardedAt].atOffset(ZoneOffset.UTC).toInstant()
        )
}