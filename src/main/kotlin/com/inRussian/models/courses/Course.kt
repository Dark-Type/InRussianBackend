package com.inRussian.models.courses

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Course(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val authorId: String,
    val authorUrl: String? = null,
    val language: String,
    val isPublished: Boolean = false,
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString()
)

