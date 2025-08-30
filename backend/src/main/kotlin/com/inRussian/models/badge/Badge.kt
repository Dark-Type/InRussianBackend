package com.inRussian.models.badge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.UUID

@Serializable
data class Badge(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val iconUrl: String,
    val badgeType: BadgeType,
    val criteria: JsonElement? = null,
    val createdAt: String
)