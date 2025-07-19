package com.inRussian.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object UserCourseEnrollments : Table("user_course_enrollments") {
    val userId = reference("user_id", Users)
    val courseId = reference("course_id", Courses)
    val enrolledAt = timestamp("enrolled_at").defaultExpression(CurrentTimestamp)
    val completedAt = timestamp("completed_at").nullable()
    val currentSectionId = reference("current_section_id", Sections).nullable()
    val currentThemeId = reference("current_theme_id", Themes).nullable()

    override val primaryKey = PrimaryKey(userId, courseId)
}