package com.inRussian.tables

import com.inRussian.models.tasks.TaskType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object Tasks : UUIDTable("tasks") {
    val themeId = reference("theme_id", Themes)
    val name = varchar("name", 255)
    val taskType = enumerationByName("task_type", 50, TaskType::class)
    val question = text("question")
    val instructions = text("instructions").nullable()
    val isTraining = bool("is_training").default(false)
    val orderNum = integer("order_num")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(themeId, orderNum)
    }
}