package com.example.models.responses

import com.example.models.course.Section
import kotlinx.serialization.Serializable

@Serializable
data class SectionWithThemes(
    val section: Section,
    val themes: List<ThemeWithTaskCount>
)