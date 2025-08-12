package com.inRussian.services

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.UserRepository
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.StaffRegisterRequest
import com.inRussian.responses.statistics.OverallStatisticsResponse
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

    suspend fun getOverallStatistics(): Result<OverallStatisticsResponse> {
        return try {
            val averageTime = adminRepository.getOverallAverageTime()
            val averageProgress = adminRepository.getOverallAverageProgress()
            val studentsCount = adminRepository.getOverallStudentsCount()
            val stats = OverallStatisticsResponse(
                totalStudents = studentsCount,
                averageTimeSpentSeconds = averageTime,
                averageProgressPercentage = averageProgress
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}