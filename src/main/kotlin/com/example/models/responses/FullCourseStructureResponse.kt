package com.example.models.responses

import com.example.models.course.Course
import kotlinx.serialization.Serializable

// Ответы с полной структурой для контент-менеджеров
@Serializable
data class FullCourseStructureResponse(
    val course: Course,
    val sections: List<FullSectionResponse>
)