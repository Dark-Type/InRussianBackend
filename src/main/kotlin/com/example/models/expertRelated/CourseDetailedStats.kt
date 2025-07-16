package com.example.models.expertRelated

import com.example.models.course.Course
import com.example.models.statistics.CourseStatistics
import kotlinx.serialization.Serializable

@Serializable
data class CourseDetailedStats(
    val course: Course,
    val statistics: CourseStatistics,
    val studentsProgress: List<StudentCourseProgress>,
    val taskDifficultyStats: List<TaskDifficultyStats>,
    val timeDistribution: CourseTimeDistribution
)