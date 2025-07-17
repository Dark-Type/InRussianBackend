package com.inRussian.tables

import com.inRussian.models.media.FileType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object MediaFiles : Table("media_files") {
    val id = varchar("id", 255)
    val fileName = varchar("file_name", 500)
    val fileType = enumerationByName("file_type", 20, FileType::class)
    val mimeType = varchar("mime_type", 100)
    val fileSize = long("file_size")
    val uploadedBy = reference("uploaded_by", Users).nullable()
    val uploadedAt = timestamp("uploaded_at").defaultExpression(CurrentTimestamp)
    val isActive = bool("is_active").default(true)
    override val primaryKey = PrimaryKey(id)
}