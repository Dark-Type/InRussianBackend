package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp


object UserStatistics : Table("user_statistics") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val totalTasksCompleted = integer("total_tasks_completed").default(0)
    val totalTasksAttempted = integer("total_tasks_attempted").default(0)
    val totalTimeSpentSeconds = integer("total_time_spent_seconds").default(0)
    val totalCorrectAnswers = integer("total_correct_answers").default(0)
    val coursesEnrolled = integer("courses_enrolled").default(0)
    val coursesCompleted = integer("courses_completed").default(0)
    val currentStreakDays = integer("current_streak_days").default(0)
    val longestStreakDays = integer("longest_streak_days").default(0)
    val lastActivityDate = date("last_activity_date").nullable()
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    override val primaryKey = PrimaryKey(userId)
}