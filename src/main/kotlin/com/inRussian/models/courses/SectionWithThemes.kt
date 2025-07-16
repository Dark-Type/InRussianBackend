package com.inRussian.models.courses

import kotlinx.serialization.Serializable

@Serializable
data class SectionWithThemes(
    val section: Section,
    val themes: List<ThemeWithTaskCount>
)