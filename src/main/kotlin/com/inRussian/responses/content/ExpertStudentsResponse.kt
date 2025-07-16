package com.inRussian.responses.content

import com.inRussian.models.expert.ExpertFilterRequest
import com.inRussian.models.expert.StudentWithActivityData
import kotlinx.serialization.Serializable

@Serializable
data class ExpertStudentsResponse(
    val students: List<StudentWithActivityData>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val filters: ExpertFilterRequest
)