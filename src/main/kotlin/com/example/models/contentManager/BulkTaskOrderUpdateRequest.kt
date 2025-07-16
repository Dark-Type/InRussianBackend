package com.example.models.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class BulkTaskOrderUpdateRequest(
    val taskOrders: List<TaskOrderItem>
)