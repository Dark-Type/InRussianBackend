package com.inRussian.models.courses

import kotlinx.serialization.Serializable

@Serializable
data class TaskOrderItem(
    val taskId: String,
    val orderNum: Int
)