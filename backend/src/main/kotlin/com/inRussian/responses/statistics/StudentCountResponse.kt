package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class StudentCountResponse(
    val totalStudents: Int
)


