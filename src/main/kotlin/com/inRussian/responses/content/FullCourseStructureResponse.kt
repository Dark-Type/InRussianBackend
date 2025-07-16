package com.inRussian.responses.content

import com.inRussian.models.courses.Course
import kotlinx.serialization.Serializable

// Ответы с полной структурой для контент-менеджеров
@Serializable
data class FullCourseStructureResponse(
    val course: Course,
    val sections: List<FullSectionResponse>
)