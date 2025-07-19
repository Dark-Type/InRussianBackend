package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object Sections : UUIDTable("sections") {
    val courseId = reference("course_id", Courses)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val orderNum = integer("order_num")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(courseId, orderNum)
    }
}