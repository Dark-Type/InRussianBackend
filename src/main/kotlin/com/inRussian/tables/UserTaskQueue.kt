package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTaskQueue : UUIDTable("user_task_queue") {
    val userId = reference("user_id", Users)
    val taskId = reference("task_id", Tasks)
    val themeId = reference("theme_id", Themes)
    val queuePosition = integer("queue_position")
    val isOriginalTask = bool("is_original_task").default(true)
    val isRetryTask = bool("is_retry_task").default(false)
    val originalTaskId = reference("original_task_id", Tasks).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}