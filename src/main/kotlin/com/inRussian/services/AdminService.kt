package com.inRussian.services

import com.inRussian.models.users.*
import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.*
import java.math.BigDecimal
import java.time.LocalDate

class AdminService(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
    private val authService: AuthService
) {

    suspend fun getAllUsers(
        page: Int = 1,
        size: Int = 20,
        role: UserRole? = null,
        createdFrom: LocalDate? = null,
        createdTo: LocalDate? = null,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): Result<List<User>> {
        return try {
            val users = adminRepository.getAllUsers(page, size, role, createdFrom, createdTo, sortBy, sortOrder)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersCount(
        role: UserRole? = null,
        createdFrom: LocalDate? = null,
        createdTo: LocalDate? = null
    ): Result<Long> {
        return try {
            val count = adminRepository.getUsersCount(role, createdFrom, createdTo)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        val user = userRepository.findById(userId)
            ?: return Result.failure(Exception("User not found"))
        return Result.success(user)
    }

    suspend fun updateUser(userId: String, request: UpdateUserRequest): Result<User> {
        return try {
            val updatedUser = adminRepository.updateUser(userId, request)
                ?: return Result.failure(Exception("User not found"))
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerStaff(request: StaffRegisterRequest): Result<com.inRussian.responses.auth.LoginResponse> {
        return authService.registerStaff(request)
    }

    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Boolean> {
        return authService.updateUserStatus(userId, status)
    }

    // Statistics methods
    suspend fun getStudentsCountByCourse(courseId: String): Result<Long> {
        return try {
            val count = adminRepository.getStudentsCountByCourse(courseId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverallStudentsCount(): Result<Long> {
        return try {
            val count = adminRepository.getOverallStudentsCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseStatistics(courseId: String): Result<Map<String, Any?>> {
        return try {
            val averageTime = adminRepository.getCourseAverageTime(courseId)
            val averageProgress = adminRepository.getCourseAverageProgress(courseId)
            val studentsCount = adminRepository.getStudentsCountByCourse(courseId)

            val stats = mapOf(
                "studentsCount" to studentsCount,
                "averageTimeSpentSeconds" to averageTime,
                "averageProgressPercentage" to averageProgress
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverallStatistics(): Result<Map<String, Any?>> {
        return try {
            val averageTime = adminRepository.getOverallAverageTime()
            val averageProgress = adminRepository.getOverallAverageProgress()
            val studentsCount = adminRepository.getOverallStudentsCount()

            val stats = mapOf(
                "totalStudents" to studentsCount,
                "averageTimeSpentSeconds" to averageTime,
                "averageProgressPercentage" to averageProgress
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun createUserProfileForUser(userId: String, request: CreateUserProfileRequest): Result<UserProfile> {
        return try {
            val profile = adminRepository.createUserProfile(userId, request)
                ?: return Result.failure(Exception("Failed to create user profile. User might not exist or not be a student."))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfileByUserId(userId: String): Result<UserProfile> {
        return try {
            val profile = adminRepository.getUserProfile(userId)
                ?: return Result.failure(Exception("User profile not found"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfileForUser(userId: String, request: UpdateUserProfileRequest): Result<UserProfile> {
        return try {
            val profile = adminRepository.updateUserProfile(userId, request)
                ?: return Result.failure(Exception("User profile not found"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfileForUser(userId: String): Result<Boolean> {
        return try {
            val deleted = adminRepository.deleteUserProfile(userId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Staff Profile methods
    suspend fun createStaffProfileForUser(userId: String, request: CreateStaffProfileRequest): Result<StaffProfile> {
        return try {
            val profile = adminRepository.createStaffProfile(userId, request)
                ?: return Result.failure(Exception("Failed to create staff profile. User might not exist or not be staff."))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffProfileByUserId(userId: String): Result<StaffProfile> {
        return try {
            val profile = adminRepository.getStaffProfile(userId)
                ?: return Result.failure(Exception("Staff profile not found"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStaffProfileForUser(userId: String, request: UpdateStaffProfileRequest): Result<StaffProfile> {
        return try {
            val profile = adminRepository.updateStaffProfile(userId, request)
                ?: return Result.failure(Exception("Staff profile not found"))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStaffProfileForUser(userId: String): Result<Boolean> {
        return try {
            val deleted = adminRepository.deleteStaffProfile(userId)
            if (deleted) {
                Result.success(true)
            } else {
                Result.failure(Exception("Staff profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}