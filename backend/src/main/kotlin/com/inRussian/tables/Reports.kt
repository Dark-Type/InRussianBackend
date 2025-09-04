package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object Reports : UUIDTable("reports") {
    val description = varchar("description", 1000)
    val taskId = reference("task_id", TaskEntity)
    val reporterId = reference("reporter_id", Users)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}