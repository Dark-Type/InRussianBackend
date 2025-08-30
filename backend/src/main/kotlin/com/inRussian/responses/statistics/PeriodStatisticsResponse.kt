package com.inRussian.responses.statistics

import kotlinx.serialization.Serializable

@Serializable
data class PeriodStatisticsResponse(
    val period: PeriodStatisticsRequest,
    val totalTasksCompleted: Int,
    val totalTimeSpentMinutes: Int,
    val totalStudentsActive: Int,
    val averageTimePerTask: Double,
    val averageProgressIncrease: Double,
    val mostActiveCourses: List<CourseActivityInPeriod>,
    val mostActiveStudents: List<StudentActivityInPeriod>
)