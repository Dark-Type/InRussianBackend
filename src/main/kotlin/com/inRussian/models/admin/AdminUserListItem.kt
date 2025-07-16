package com.inRussian.models.admin

import com.inRussian.models.statistics.UserStatistics
import com.inRussian.models.users.StaffProfile
import com.inRussian.models.users.User
import com.inRussian.models.users.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class AdminUserListItem(
    val user: User,
    val studentProfile: UserProfile? = null,
    val staffProfile: StaffProfile? = null,
    val statistics: UserStatistics? = null
)