package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object UserThemeQueueItemTable : Table("user_theme_queue_item") {
    val userId = uuid("user_id")
    val themeId = uuid("theme_id")
    val taskId = uuid("task_id")

    // Ordering within the user's queue for this theme (lower comes first)
    val position = integer("position")

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    // Uniqueness: a task should appear at most once per user per theme
    override val primaryKey =
        PrimaryKey(userId, themeId, taskId, name = "pk_user_theme_queue_item")

    init {
        index(isUnique = false, columns = arrayOf(userId, themeId, position))

        index(isUnique = false, columns = arrayOf(userId, themeId))
    }
}