package com.inRussian.repositories.v2

import com.inRussian.tables.v2.BadgeRuleTable
import com.inRussian.tables.v2.SimpleBadgeRuleType
import com.inRussian.tables.v2.UserBadgeTable
import com.inRussian.tables.v2.UserDailySolveTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.and
import kotlin.compareTo
import kotlin.text.get
import kotlin.text.set
import kotlin.time.ExperimentalTime

class BadgeRepository {

    suspend fun listActiveRules(): List<ResultRow> = newSuspendedTransaction(Dispatchers.IO) {
        BadgeRuleTable.selectAll().where { BadgeRuleTable.active eq true }.toList()
    }

    suspend fun listActiveRulesByType(type: SimpleBadgeRuleType): List<ResultRow> = newSuspendedTransaction(Dispatchers.IO) {
        BadgeRuleTable.selectAll().where { (BadgeRuleTable.active eq true) and (BadgeRuleTable.type eq type) }.toList()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun awardIfAbsent(
        userId: UUID,
        badgeId: UUID,
        sectionId: UUID? = null,
        courseId: UUID? = null
    ): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val inserted = UserBadgeTable.insertIgnore {
            it[UserBadgeTable.userId] = userId
            it[UserBadgeTable.badgeId] = badgeId
            it[UserBadgeTable.sectionId] = sectionId
            it[UserBadgeTable.courseId] = courseId
            it[UserBadgeTable.awardedAt] = Instant.now()
        }.insertedCount
        inserted > 0
    }

    suspend fun recordDailySolve(userId: UUID, dayUtc: LocalDate = LocalDate.now(ZoneOffset.UTC)): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        val inserted = UserDailySolveTable.insertIgnore {
            it[UserDailySolveTable.userId] = userId
            it[UserDailySolveTable.day] = dayUtc
        }.insertedCount
        inserted > 0
    }

    suspend fun currentDailyStreak(userId: UUID, todayUtc: LocalDate = LocalDate.now(ZoneOffset.UTC)): Int = newSuspendedTransaction(Dispatchers.IO) {
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