package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object UserBadges : Table("user_badges") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val badgeId = reference("badge_id", Badges)
    val earnedAt = timestamp("earned_at").defaultExpression(CurrentTimestamp)
    val courseId = reference("course_id", Courses).nullable()
    val themeId = reference("theme_id", Themes).nullable()

    override val primaryKey = PrimaryKey(userId, badgeId)
}