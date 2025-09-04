package com.inRussian.tables.v2

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTaskAttemptTable : UUIDTable("user_task_attempt") {
    val userId = uuid("user_id").index()
    val taskId = uuid("task_id").index()
    val themeId = uuid("theme_id").index()
    val courseId = uuid("course_id").index()

    val attemptsCount = integer("attempts_count")
    val timeSpentMs = long("time_spent_ms")

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    init {
        index(false, userId, courseId)
        index(false, taskId, userId)
    }
}