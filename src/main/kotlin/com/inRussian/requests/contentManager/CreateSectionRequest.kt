package com.inRussian.requests.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class CreateSectionRequest(
    val courseId: String,
    val name: String,
    val description: String? = null,
    val orderNum: Int
)

