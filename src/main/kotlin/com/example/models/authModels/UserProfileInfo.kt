package com.example.models.authModels

import com.example.models.user.StaffProfile
import com.example.models.user.UserProfile
import kotlinx.serialization.Serializable

@Serializable
sealed class UserProfileInfo {
    @Serializable
    data class StudentInfo(val profile: UserProfile) : UserProfileInfo()

    @Serializable
    data class StaffInfo(val profile: StaffProfile) : UserProfileInfo()
}