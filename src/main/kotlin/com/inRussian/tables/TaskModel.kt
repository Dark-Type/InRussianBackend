package com.inRussian.tables

import com.inRussian.models.tasks.TaskType
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.json.jsonb

object TaskModel : UUIDTable("task_model") {
    val taskType = enumerationByName("task_type", 50, TaskType::class)
    val courseId = reference("course_id", Courses)
    val taskBody = jsonb<JsonElement>("task_body", json)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}