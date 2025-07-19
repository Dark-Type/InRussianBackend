package com.inRussian.tables

import com.inRussian.models.tasks.ContentType
import org.jetbrains.exposed.dao.id.UUIDTable


object TaskContent : UUIDTable("task_content") {
    val taskId = reference("task_id", Tasks)
    val contentType = enumerationByName("content_type", 20, ContentType::class)
    val contentId = varchar("content_id", 255).nullable()
    val description = text("description").nullable()
    val transcription = text("transcription").nullable()
    val translation = text("translation").nullable()
    val orderNum = integer("order_num")
}