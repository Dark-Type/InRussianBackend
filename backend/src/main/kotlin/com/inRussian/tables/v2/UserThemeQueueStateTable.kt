package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object UserThemeQueueStateTable : Table("user_theme_queue_state") {
    val userId = uuid("user_id")
    val themeId = uuid("theme_id")

    // Monotonically increasing position used to append items to the end of the queue
    val lastPosition = integer("last_position").default(0)

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(userId, themeId, name = "pk_user_theme_queue_state")

    init {
        // Helpful for lookups
        index(isUnique = false, columns = arrayOf(userId, themeId))
    }
}