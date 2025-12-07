package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object UserCourseEnrollments : Table("user_course_enrollments") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val courseId = reference("course_id", Courses)
    val enrolledAt = timestamp("enrolled_at").defaultExpression(CurrentTimestamp)
    val completedAt = timestamp("completed_at").nullable()
    val progress = decimal("progress", 5, 2).default(0.00.toBigDecimal())

    override val primaryKey = PrimaryKey(userId, courseId)
}