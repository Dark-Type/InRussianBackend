package com.inRussian.models.expert

import com.inRussian.models.admin.SortDirection
import kotlinx.serialization.Serializable

@Serializable
data class ExpertFilterRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val courseId: String? = null,
    val sortBy: ExpertSortField = ExpertSortField.LAST_ACTIVITY,
    val sortDirection: SortDirection = SortDirection.DESC,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val searchQuery: String? = null,
    val minProgress: Double? = null,
    val maxProgress: Double? = null
)