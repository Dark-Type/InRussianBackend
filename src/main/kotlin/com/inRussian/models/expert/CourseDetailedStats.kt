package com.inRussian.models.expert

import com.inRussian.models.courses.Course
import com.inRussian.models.statistics.CourseStatistics
import kotlinx.serialization.Serializable

@Serializable
data class CourseDetailedStats(
    val course: Course,
    val statistics: CourseStatistics,
    val studentsProgress: List<StudentCourseProgress>,
    val taskDifficultyStats: List<TaskDifficultyStats>,
    val timeDistribution: CourseTimeDistribution
)