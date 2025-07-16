package com.inRussian.models.expert

import com.inRussian.models.statistics.UserStatistics
import com.inRussian.models.users.User
import com.inRussian.models.users.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class StudentWithActivityData(
    val user: User,
    val profile: UserProfile,
    val statistics: UserStatistics,
    val courseProgress: List<StudentCourseProgress> = emptyList()
)