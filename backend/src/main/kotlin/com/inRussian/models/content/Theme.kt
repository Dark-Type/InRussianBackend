package com.inRussian.models.content

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val id: String,
    val courseId: String,
    val parentThemeId: String?,
    val name: String,
    val description: String?,
    val position: Int,
    val createdAt: String
)