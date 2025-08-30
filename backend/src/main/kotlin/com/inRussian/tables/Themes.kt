package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object Themes : UUIDTable("themes") {
    val sectionId = reference("section_id", Sections)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val orderNum = integer("order_num")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    init {
        uniqueIndex(sectionId, orderNum)
    }
}