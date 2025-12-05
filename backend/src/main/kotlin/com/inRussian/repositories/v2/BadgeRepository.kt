package com.inRussian.repositories.v2

import com.inRussian.config.DatabaseFactory.dbQuery
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import com.inRussian.tables.v2.UserBadgeTable
import com.inRussian.tables.v2.UserDailySolveTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID


class BadgeRepository {

    suspend fun listActiveRules(): List<ResultRow> = dbQuery {
        BadgeRuleTable.selectAll().where { BadgeRuleTable.active eq true }.toList()
    }

    suspend fun listActiveRulesByType(type: SimpleBadgeRuleType): List<ResultRow> = dbQuery {
        BadgeRuleTable.selectAll().where { (BadgeRuleTable.active eq true) and (BadgeRuleTable.type eq type) }.toList()
    }

    suspend fun awardIfAbsent(
        userId: UUID,
        badgeId: UUID,
        courseId: UUID? = null,
        themeId: UUID? = null
    ): Boolean = dbQuery {
        val inserted = UserBadgeTable.insertIgnore {
            it[UserBadgeTable.userId] = userId
            it[UserBadgeTable.badgeId] = badgeId
            it[UserBadgeTable.courseId] = courseId
            it[UserBadgeTable.themeId] = themeId
            it[UserBadgeTable.awardedAt] = Instant.now()
        }.insertedCount
        inserted > 0
    }

    suspend fun recordDailySolve(userId: UUID, dayUtc: LocalDate = LocalDate.now(ZoneOffset.UTC)): Boolean =
        dbQuery {
            val inserted = UserDailySolveTable.insertIgnore {
                it[UserDailySolveTable.userId] = userId
                it[UserDailySolveTable.day] = dayUtc
            }.insertedCount
            inserted > 0
        }

    suspend fun currentDailyStreak(userId: UUID, todayUtc: LocalDate = LocalDate.now(ZoneOffset.UTC)): Int =
        dbQuery {
            val days = UserDailySolveTable
                .selectAll()
                .where { (UserDailySolveTable.userId eq userId) and (UserDailySolveTable.day lessEq todayUtc) }
                .orderBy(UserDailySolveTable.day to SortOrder.DESC)
                .limit(60)
                .map { it[UserDailySolveTable.day] }
                .toSet()

            var streak = 0
            var day = todayUtc
            while (days.contains(day)) {
                streak += 1
                day = day.minusDays(1)
            }
            streak
        }

    fun listUserBadges(userId: UUID): List<ResultRow> =
        UserBadgeTable
            .selectAll().where { UserBadgeTable.userId eq userId }
            .orderBy(UserBadgeTable.awardedAt to SortOrder.DESC)
            .toList()
}