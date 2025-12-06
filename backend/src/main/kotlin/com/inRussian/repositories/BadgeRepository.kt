package com.inRussian.repositories

import com.inRussian.config.DatabaseFactory
import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import com.inRussian.tables.v2.UserBadgeTable
import com.inRussian.tables.v2.UserDailySolveTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

interface BadgeRepository {
    suspend fun listActiveRules(): List<ResultRow>
    suspend fun listActiveRulesByType(type: SimpleBadgeRuleType): List<ResultRow>

    suspend fun awardIfAbsent(
        userId: UUID,
        badgeId: UUID,
        courseId: UUID? = null,
        themeId: UUID? = null
    ): Boolean

    suspend fun recordDailySolve(userId: UUID, dayUtc: LocalDate): Boolean

    suspend fun currentDailyStreak(userId: UUID, todayUtc: LocalDate): Int

    suspend fun listUserBadges(userId: UUID): List<ResultRow>
}

class ExposedBadgeRepository : com.inRussian.repositories.BadgeRepository {

    override suspend fun listActiveRules(): List<ResultRow> = DatabaseFactory.dbQuery {
        val query = BadgeRuleTable.selectAll()
        query.andWhere { BadgeRuleTable.active eq true }
        query.toList()
    }

    override suspend fun listActiveRulesByType(type: SimpleBadgeRuleType): List<ResultRow> = DatabaseFactory.dbQuery {
        val query = BadgeRuleTable.selectAll()
        query.andWhere { BadgeRuleTable.active eq true }
        query.andWhere { BadgeRuleTable.type eq type }
        query.toList()
    }

    override suspend fun awardIfAbsent(
        userId: UUID,
        badgeId: UUID,
        courseId: UUID?,
        themeId: UUID?
    ): Boolean = DatabaseFactory.dbQuery {
        val inserted = UserBadgeTable.insertIgnore {
            it[UserBadgeTable.userId] = userId
            it[UserBadgeTable.badgeId] = badgeId
            it[UserBadgeTable.courseId] = courseId
            it[UserBadgeTable.themeId] = themeId
            it[UserBadgeTable.awardedAt] = Instant.now()
        }.insertedCount
        inserted > 0
    }

    override suspend fun recordDailySolve(userId: UUID, dayUtc: LocalDate): Boolean =
        DatabaseFactory.dbQuery {
            val inserted = UserDailySolveTable.insertIgnore {
                it[UserDailySolveTable.userId] = userId
                it[UserDailySolveTable.day] = dayUtc
            }.insertedCount
            inserted > 0
        }

    override suspend fun currentDailyStreak(userId: UUID, todayUtc: LocalDate): Int =
        DatabaseFactory.dbQuery {
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

    override suspend fun listUserBadges(userId: UUID): List<ResultRow> = DatabaseFactory.dbQuery {
        UserBadgeTable
            .selectAll()
            .where { UserBadgeTable.userId eq userId }
            .orderBy(UserBadgeTable.awardedAt to SortOrder.DESC)
            .toList()
    }
}