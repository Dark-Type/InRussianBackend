package com.example.models.responses

import com.example.models.course.Theme
import kotlinx.serialization.Serializable

@Serializable
data class FullThemeResponse(
    val theme: Theme,
    val tasks: List<FullTaskResponse>
)