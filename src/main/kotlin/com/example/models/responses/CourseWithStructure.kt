package com.example.models.responses

import com.example.models.course.Course
import kotlinx.serialization.Serializable

@Serializable
data class CourseWithStructure(
    val course: Course,
    val sections: List<SectionWithThemes>
)