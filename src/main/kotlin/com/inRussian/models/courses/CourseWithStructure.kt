package com.inRussian.models.courses

import kotlinx.serialization.Serializable

@Serializable
data class CourseWithStructure(
    val course: Course,
    val sections: List<SectionWithThemes>
)