package com.inRussian.tables

import com.inRussian.models.users.SystemLanguage
import com.inRussian.models.users.UserRole
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp


object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 50).nullable()
    val role = enumerationByName("role", 20, UserRole::class)
    val systemLanguage = enumerationByName("system_language", 20, SystemLanguage::class)
    val avatarId = varchar("avatar_id", 255).nullable()
    val lastActivityAt = timestamp("last_activity_at").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}


val json = Json { ignoreUnknownKeys = true }


