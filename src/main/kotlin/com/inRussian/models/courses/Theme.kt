package com.inRussian.models.courses

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Theme(
    val id: String,
    val sectionId: String,
    val name: String,
    val description: String?,
    val orderNum: Int,
    val createdAt: String
)