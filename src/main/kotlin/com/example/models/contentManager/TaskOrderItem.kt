package com.example.models.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class TaskOrderItem(
    val taskId: String,
    val orderNum: Int
)