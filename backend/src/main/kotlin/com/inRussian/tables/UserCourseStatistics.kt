package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object UserCourseStatistics : Table("user_course_statistics") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val courseId = reference("course_id", Courses, onDelete = ReferenceOption.CASCADE)
    val tasksCompleted = integer("tasks_completed").default(0)
    val tasksAttempted = integer("tasks_attempted").default(0)
    val tasksTotal = integer("tasks_total").nullable()
    val timeSpentSeconds = integer("time_spent_seconds").default(0)
    val correctAnswers = integer("correct_answers").default(0)
    val progressPercentage = decimal("progress_percentage", 5, 2).default(java.math.BigDecimal.ZERO)
    val startedAt = timestamp("started_at").nullable()
    val lastActivityAt = timestamp("last_activity_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(userId, courseId)
}