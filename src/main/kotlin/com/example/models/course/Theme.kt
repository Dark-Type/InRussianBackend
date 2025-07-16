package com.example.models.course

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Theme(
    val id: String = UUID.randomUUID().toString(),
    val sectionId: String,
    val name: String,
    val description: String? = null,
    val orderNum: Int,
    val createdAt: String = LocalDateTime.now().toString()
)