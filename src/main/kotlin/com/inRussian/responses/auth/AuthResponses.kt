package com.inRussian.responses.auth

import com.inRussian.models.users.StaffProfile
import com.inRussian.models.users.User
import com.inRussian.models.users.UserInfo
import com.inRussian.models.users.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val code: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class MessageResponse(
    val success: Boolean = true,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LoginResponse(
    val success: Boolean = true,
    val accessToken: String,
    val user: UserInfo,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val role: String,
    val phone: String? = null,
    val systemLanguage: String,
    val status: String
)

@Serializable
data class UserInfoData(
    val id: String,
    val email: String,
    val role: String
)

@Serializable
data class UserInfoResponse(
    val success: Boolean = true,
    val user: UserInfoData,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UserUpdateResponse(
    val success: Boolean = true,
    val user: User,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class UserProfileResponse(
    val success: Boolean = true,
    val profile: UserProfile,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class StaffProfileResponse(
    val success: Boolean = true,
    val profile: StaffProfile,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AdminCreatedResponse(
    val success: Boolean = true,
    val email: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)