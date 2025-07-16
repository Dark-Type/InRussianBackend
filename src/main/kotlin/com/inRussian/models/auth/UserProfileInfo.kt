package com.inRussian.models.auth

import com.inRussian.models.users.StaffProfile
import com.inRussian.models.users.UserProfile
import kotlinx.serialization.Serializable

@Serializable
sealed class UserProfileInfo {
    @Serializable
    data class StudentInfo(val profile: UserProfile) : UserProfileInfo()

    @Serializable
    data class StaffInfo(val profile: StaffProfile) : UserProfileInfo()
}