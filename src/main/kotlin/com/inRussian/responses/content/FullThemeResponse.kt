package com.inRussian.responses.content

import com.inRussian.models.courses.Theme
import kotlinx.serialization.Serializable

@Serializable
data class FullThemeResponse(
    val theme: Theme,
    val tasks: List<FullTaskResponse>
)