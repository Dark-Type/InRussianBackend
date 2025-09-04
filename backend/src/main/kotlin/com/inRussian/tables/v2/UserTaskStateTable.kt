package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

enum class TaskStatus { NEW, IN_PROGRESS, SOLVED }

object UserTaskStateTable : Table("user_task_state") {
    val userId = uuid("user_id")
    val taskId = uuid("task_id")

    val themeId = uuid("theme_id").index()
    val courseId = uuid("course_id").index()

    val isSolvedFirstTry = bool("is_solved_first_try").default(false)
    val firstSolvedAt = timestamp("first_solved_at").nullable()

    override val primaryKey = PrimaryKey(userId, taskId, name = "pk_user_task_state")

    init {
        index(false, userId, courseId)
    }
}