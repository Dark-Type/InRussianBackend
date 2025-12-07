package com.inRussian.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object CourseStatistics : Table("course_statistics") {
    val courseId = reference("course_id", Courses, onDelete = ReferenceOption.CASCADE)
    val studentsEnrolled = integer("students_enrolled").default(0)
    val studentsCompleted = integer("students_completed").default(0)
    val studentsActiveLast7Days = integer("students_active_last_7_days").default(0)
    val totalTasksCompleted = integer("total_tasks_completed").default(0)
    val totalTimeSpentSeconds = integer("total_time_spent_seconds").default(0)
    val totalAttempts = integer("total_attempts").default(0)
    val totalCorrectAnswers = integer("total_correct_answers").default(0)
    val averageCompletionTimeSeconds = integer("average_completion_time_seconds").default(0)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    override val primaryKey = PrimaryKey(courseId)
}