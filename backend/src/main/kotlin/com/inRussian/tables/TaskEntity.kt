package com.inRussian.tables

import com.inRussian.config.appJson
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object TaskEntity : UUIDTable("task_entity") {
    val themeId = reference("theme_id", Themes, onDelete = ReferenceOption.CASCADE)
    val taskBody = jsonb<JsonElement>("task_body", appJson)
    val question = text("question")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}