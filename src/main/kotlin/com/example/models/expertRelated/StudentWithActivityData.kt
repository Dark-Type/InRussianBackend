package com.example.models.expertRelated

import com.example.models.statistics.UserStatistics
import com.example.models.user.User
import com.example.models.user.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class StudentWithActivityData(
    val user: User,
    val profile: UserProfile,
    val statistics: UserStatistics,
    val courseProgress: List<StudentCourseProgress> = emptyList()
)