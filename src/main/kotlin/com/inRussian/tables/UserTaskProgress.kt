package com.inRussian.tables

import com.inRussian.models.progress.TaskStatus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserTaskProgress : Table("user_task_progress") {
    val userId = reference("user_id", Users)
    val taskId = reference("task_id", Tasks)
    val status = enumerationByName("status", 20, TaskStatus::class).default(TaskStatus.NOT_STARTED)
    val attemptCount = integer("attempt_count").default(0)
    val isCorrect = bool("is_correct").nullable()
    val lastAttemptAt = timestamp("last_attempt_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
    val shouldRetryAfterTasks = integer("should_retry_after_tasks").nullable()

    override val primaryKey = PrimaryKey(userId, taskId)
}