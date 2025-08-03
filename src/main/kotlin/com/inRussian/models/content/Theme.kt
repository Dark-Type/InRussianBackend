package com.inRussian.models.content

import kotlinx.serialization.Serializable

@Serializable
data class Theme(
    val id: String,
    val sectionId: String,
    val name: String,
    val description: String?,
    val orderNum: Int,
    val createdAt: String
)