package com.inRussian.models.expert

import kotlinx.serialization.Serializable

@Serializable
data class ExpertDashboardData(
    val totalStudentsCount: Int,
    val activeStudentsLast7Days: Int,
    val courseStatistics: List<CourseOverviewStats>,
    val topActiveStudents: List<StudentActivitySummary>,
    val systemOverview: SystemOverviewStats
)