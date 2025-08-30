package com.inRussian.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object MediaFiles : UUIDTable("media_files") {
    val fileName = varchar("file_name", 500)
    val fileType = varchar("file_type", 20)
    val mimeType = varchar("mime_type", 100)
    val fileSize = long("file_size")
    val uploadedBy = reference("uploaded_by", Users).nullable()
    val uploadedAt = timestamp("uploaded_at").defaultExpression(CurrentTimestamp)
    val isActive = bool("is_active").default(true)
}