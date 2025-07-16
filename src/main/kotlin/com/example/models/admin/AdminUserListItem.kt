package com.example.models.admin

import com.example.models.statistics.UserStatistics
import com.example.models.user.StaffProfile
import com.example.models.user.User
import com.example.models.user.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserListItem(
    val user: User,
    val studentProfile: UserProfile? = null,
    val staffProfile: StaffProfile? = null,
    val statistics: UserStatistics? = null
)