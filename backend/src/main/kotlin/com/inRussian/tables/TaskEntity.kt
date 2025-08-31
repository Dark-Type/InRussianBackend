package com.inRussian.tables

import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object TaskEntity : UUIDTable("task_entity") {
    val themeId = reference("theme_id", Themes)
    val taskBody = jsonb<JsonElement>("task_body", json)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}