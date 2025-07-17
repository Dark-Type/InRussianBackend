package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Courses : UUIDTable("courses") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val authorId = reference("author_id", Users)
    val authorUrl = varchar("author_url", 500).nullable()
    val language = varchar("language", 50)
    val isPublished = bool("is_published").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}