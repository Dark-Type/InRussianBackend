package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
enum class ExpertSortField {
    LAST_ACTIVITY, TOTAL_TIME_SPENT, PROGRESS, CREATED_AT, TASKS_COMPLETED
}