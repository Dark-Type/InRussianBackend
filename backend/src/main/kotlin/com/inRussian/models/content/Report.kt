package com.inRussian.models.content

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String,
    val description: String,
    val taskId: String,
    val reporterId: String,
    val createdAt: String
)