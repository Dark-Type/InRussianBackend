package com.inRussian.tables

import com.inRussian.models.badge.BadgeType
import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.json.jsonb


object Badges : UUIDTable("badges") {
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val imageId = varchar("image_id", 255)
    val badgeType = enumerationByName("badge_type", 30, BadgeType::class)
    val criteria = jsonb<JsonElement>("criteria", json)
}