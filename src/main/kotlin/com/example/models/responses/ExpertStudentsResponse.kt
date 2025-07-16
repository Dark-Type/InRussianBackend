package com.example.models.responses

import com.example.models.expertRelated.ExpertFilterRequest
import com.example.models.expertRelated.StudentWithActivityData
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