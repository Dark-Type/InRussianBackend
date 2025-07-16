package com.inRussian.requests.contentManager

import kotlinx.serialization.Serializable

@Serializable
data class UpdateThemeRequest(
    val name: String? = null,
    val description: String? = null,
    val orderNum: Int? = null
)