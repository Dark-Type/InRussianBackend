package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object Themes : UUIDTable("themes") {
    val courseId = reference("course_id", Courses, onDelete = ReferenceOption.CASCADE)
    val parentThemeId = reference("parent_theme_id", this, onDelete = ReferenceOption.CASCADE).nullable()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val position = integer("position").default(0)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    init {

        index(isUnique = true, parentThemeId, position)
    }
}