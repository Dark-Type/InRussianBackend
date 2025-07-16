package com.example.models.expertRelated

import kotlinx.serialization.Serializable

@Serializable
enum class ExpertSortField {
    LAST_ACTIVITY, TOTAL_TIME_SPENT, PROGRESS, CREATED_AT, TASKS_COMPLETED
}