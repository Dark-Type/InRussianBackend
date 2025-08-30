package com.inRussian.requests.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class CreateCourseRequest(
    val name: String,
    val description: String? = null,
    val authorUrl: String? = null,
    val language: String
)