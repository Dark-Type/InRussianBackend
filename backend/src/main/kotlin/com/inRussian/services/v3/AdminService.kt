package com.inRussian.services.v3

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserStatus
import com.inRussian.repositories.ProgressStatsRepository
import com.inRussian.repositories.UserCourseEnrollmentsRepository
import com.inRussian.repositories.UserSortBy
import com.inRussian.repositories.UsersRepository
import com.inRussian.requests.admin.UpdateUserRequest
import com.inRussian.requests.users.StaffRegisterRequest
import com.inRussian.responses.auth.LoginResponse
import com.inRussian.responses.statistics.OverallCourseStatisticsResponse
import com.inRussian.responses.statistics.OverallStatisticsResponse
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDate
import java.util.UUID

class AdminService(
    private val userRepository: UsersRepository,
    private val authService: AuthService,
    private val enrollmentsRepository: UserCourseEnrollmentsRepository,
    private val progressStatsRepository: ProgressStatsRepository,
) {

    private fun parseUserSortBy(sortBy: String): UserSortBy =
        when (sortBy.lowercase()) {
            "email" -> UserSortBy.EMAIL
            "role" -> UserSortBy.ROLE
            "status" -> UserSortBy.STATUS
            "last_activity_at", "lastactivityat", "last_activity" -> UserSortBy.LAST_ACTIVITY_AT
            "created_at", "createdat", "created" -> UserSortBy.CREATED_AT
            else -> UserSortBy.CREATED_AT
        }

    private fun parseSortOrder(sortOrder: String): SortOrder =
        when (sortOrder.lowercase()) {
            "asc" -> SortOrder.ASC
            else -> SortOrder.DESC
        }

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
            val sortByEnum = parseUserSortBy(sortBy)
            val sortOrderEnum = parseSortOrder(sortOrder)
            val users = userRepository.findAll(
                page = page,
                size = size,
                role = role,
                createdFrom = createdFrom,
                createdTo = createdTo,
                sortBy = sortByEnum,
                sortOrder = sortOrderEnum
            )
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
            val count = userRepository.count(role, createdFrom, createdTo)
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
            val updatedUser = userRepository.update(userId, request)
                ?: return Result.failure(Exception("User not found"))
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerStaff(request: StaffRegisterRequest): Result<LoginResponse> {
        return authService.registerStaff(request)
    }

    suspend fun updateUserStatus(userId: String, status: UserStatus): Result<Boolean> {
        return authService.updateUserStatus(userId, status)
    }

    suspend fun getStudentsCountByCourse(courseId: String): Result<Long> {
        return try {
            val count = enrollmentsRepository.countByCourse(courseId)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverallStudentsCount(): Result<Long> {
        return try {
            val count = userRepository.countRole(UserRole.STUDENT)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCourseStatistics(courseId: String): Result<OverallCourseStatisticsResponse> {
        return try {
            val courseUuid = UUID.fromString(courseId)

            val avgProgressDto = progressStatsRepository.getCourseAverageProgress(courseUuid)
            val studentsCount = enrollmentsRepository.countByCourse(courseId)

            // Переводим среднее время из миллисекунд в секунды
            val avgTimeSeconds = (avgProgressDto.averageTimeMsAvg / 1000.0)

            val stats = OverallCourseStatisticsResponse(
                studentsCount = studentsCount,
                averageTimeSpentSeconds = avgTimeSeconds.toLong(),
                averageProgressPercentage = avgProgressDto.percentAvg
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOverallStatistics(): Result<OverallStatisticsResponse> {
        return try {
            val platformAvg = progressStatsRepository.getPlatformCourseAverages()
            val studentsCount = userRepository.countRole(UserRole.STUDENT)

            val avgTimeSeconds = (platformAvg.averageTimeMsAvg / 1000.0)

            val stats = OverallStatisticsResponse(
                totalStudents = studentsCount,
                averageTimeSpentSeconds = avgTimeSeconds.toLong(),
                averageProgressPercentage = platformAvg.percentAvg
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}