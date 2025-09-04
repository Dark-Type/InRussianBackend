package com.inRussian.tables.v2

import com.inRussian.tables.Courses
import com.inRussian.tables.Themes
import com.inRussian.tables.Users
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserThemeProgressTable : Table("user_theme_progress") {
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val themeId = uuid("theme_id").references(Themes.id, onDelete = ReferenceOption.CASCADE)
    val courseId = uuid("course_id").references(Courses.id, onDelete = ReferenceOption.CASCADE)

    val solvedTasks = integer("solved_tasks").default(0)
    val totalTasks = integer("total_tasks").default(0)
    val totalTimeMs = long("total_time_ms").default(0L)
    val averageTimeMs = integer("average_time_ms").default(0)
    val percentComplete = double("percent_complete").default(0.0)

    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(userId, themeId, name = "pk_user_theme_progress")

}