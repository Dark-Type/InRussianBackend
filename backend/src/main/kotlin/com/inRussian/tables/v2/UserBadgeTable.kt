package com.inRussian.tables.v2

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp


enum class SimpleBadgeRuleType {
    THEME_COMPLETED, COURSE_COMPLETED, DAILY_STREAK
}

object BadgeRuleTable : UUIDTable("badge_rule") {
    val badgeId = uuid("badge_id").index()
    val type = enumerationByName("type", 64, SimpleBadgeRuleType::class)
    val courseId = uuid("course_id").nullable().index()
    val streakDays = integer("streak_days").nullable()
    val active = bool("active").default(true)
    val themeId = uuid("theme_id").nullable().index()
}

object UserBadgeTable : UUIDTable("user_badge") {
    val userId = uuid("user_id").index()
    val badgeId = uuid("badge_id").index()
    val courseId = uuid("course_id").nullable().index()
    val themeId = uuid("theme_id").nullable().index()
    val awardedAt = timestamp("awarded_at")

    init {
        uniqueIndex("uq_user_badge_context", userId, badgeId, courseId)
    }
}

/**
 * For daily streak computation: record a row per day with at least one first-try solve.
 */
object UserDailySolveTable : Table("user_daily_solve") {
    val userId = uuid("user_id")
    val day = date("day")

    override val primaryKey = PrimaryKey(userId, day, name = "pk_user_daily_solve")
}