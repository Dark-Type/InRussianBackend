package com.inRussian.requests.contentManager

import com.inRussian.models.courses.TaskOrderItem
import kotlinx.serialization.Serializable

@Serializable
data class BulkTaskOrderUpdateRequest(
    val taskOrders: List<TaskOrderItem>
)