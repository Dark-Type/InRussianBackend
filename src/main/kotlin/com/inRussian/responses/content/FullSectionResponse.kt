package com.inRussian.responses.content

import com.inRussian.models.courses.Section
import kotlinx.serialization.Serializable

@Serializable
data class FullSectionResponse(
    val section: Section,
    val themes: List<FullThemeResponse>
)