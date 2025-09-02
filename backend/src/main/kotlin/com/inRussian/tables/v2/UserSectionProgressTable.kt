package com.inRussian.tables.v2

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserSectionProgressTable : Table("user_section_progress") {
    val userId = uuid("user_id")
    val sectionId = uuid("section_id")
    val courseId = uuid("course_id").index()

    val solvedTasks = integer("solved_tasks").default(0)
    val totalTasks = integer("total_tasks").default(0)
    val percentComplete = double("percent_complete").default(0.0)

    val totalTimeMs = long("total_time_ms").default(0L) // sum of first-try times
    val averageTimeMs = integer("average_time_ms").default(0)
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId, sectionId, name = "pk_user_section_progress")
}

