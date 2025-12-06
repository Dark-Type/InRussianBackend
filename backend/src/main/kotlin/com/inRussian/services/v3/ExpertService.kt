package com.inRussian.services.v3

import com.inRussian.models.users.User
import com.inRussian.models.users.UserProfile
import com.inRussian.models.users.UserRole
import com.inRussian.repositories.UserCourseEnrollmentItem
import com.inRussian.repositories.UserCourseEnrollmentsRepository
import com.inRussian.repositories.UserCourseStatisticsRepository
import com.inRussian.repositories.UserSortBy
import com.inRussian.repositories.UserStatisticsRepository
import com.inRussian.repositories.UsersRepository
import org.jetbrains.exposed.sql.SortOrder
import java.time.LocalDate

interface ExpertService {
    suspend fun getAllStudents(
        page: Int = 1,
        size: Int = 20,
        createdFrom: LocalDate? = null,
        createdTo: LocalDate? = null,
        sortBy: String = "createdAt",
        sortOrder: String = "desc"
    ): Result<List<User>>

    suspend fun getStudentsCount(createdFrom: LocalDate? = null, createdTo: LocalDate? = null): Result<Long>
    suspend fun getStudentsWithProfiles(page: Int = 1, size: Int = 20): Result<List<Pair<User, UserProfile?>>>
    suspend fun getStudentsByCourse(courseId: String): Result<List<User>>
    suspend fun getStudentsCountByCourse(courseId: String): Result<Long>
    suspend fun getOverallStudentsCount(): Result<Long>
    suspend fun getCourseAverageTime(courseId: String): Result<Long?>
    suspend fun getCourseAverageProgress(courseId: String): Result<Double?>
    suspend fun getOverallAverageTime(): Result<Long?>
    suspend fun getOverallAverageProgress(): Result<Double?>
    suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>>
    suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?>
    suspend fun unenrollStudentFromCourse(userId: String, courseId: String): Result<Boolean>
}

class ExpertServiceImpl(
    private val usersRepository: UsersRepository,
    private val userCourseEnrollmentsRepository: UserCourseEnrollmentsRepository,
    private val userCourseStatisticsRepository: UserCourseStatisticsRepository,
    private val userStatisticsRepository: UserStatisticsRepository
) : ExpertService {

    override suspend fun getAllStudents(
        page: Int,
        size: Int,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): Result<List<User>> = runCatching {
        usersRepository.findAll(
            page = page,
            size = size,
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo,
            sortBy = resolveSortBy(sortBy),
            sortOrder = resolveSortOrder(sortOrder)
        )
    }

    override suspend fun getStudentsCount(createdFrom: LocalDate?, createdTo: LocalDate?): Result<Long> = runCatching {
        usersRepository.count(
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo
        )
    }

    override suspend fun getStudentsWithProfiles(page: Int, size: Int): Result<List<Pair<User, UserProfile?>>> = runCatching {
        userCourseEnrollmentsRepository.listStudentsWithProfiles(page, size)
    }

    override suspend fun getStudentsByCourse(courseId: String): Result<List<User>> = runCatching {
        userCourseEnrollmentsRepository.listStudentsByCourse(courseId)
    }

    override suspend fun getStudentsCountByCourse(courseId: String): Result<Long> = runCatching {
        userCourseEnrollmentsRepository.countByCourse(courseId)
    }

    override suspend fun getOverallStudentsCount(): Result<Long> = runCatching {
        usersRepository.countRole(UserRole.STUDENT)
    }

    override suspend fun getCourseAverageTime(courseId: String): Result<Long?> = runCatching {
        userCourseStatisticsRepository.averageTimeByCourse(courseId)
    }

    override suspend fun getCourseAverageProgress(courseId: String): Result<Double?> = runCatching {
        userCourseStatisticsRepository.averageProgressByCourse(courseId)
    }

    override suspend fun getOverallAverageTime(): Result<Long?> = runCatching {
        userStatisticsRepository.overallAverageTime()
    }

    override suspend fun getOverallAverageProgress(): Result<Double?> = runCatching {
        userStatisticsRepository.overallAverageProgress()
    }

    override suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>> = runCatching {
        userCourseEnrollmentsRepository.getUserEnrollments(userId)
    }

    override suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?> = runCatching {
        userCourseEnrollmentsRepository.getCourseEnrollment(userId, courseId)
    }

    override suspend fun unenrollStudentFromCourse(userId: String, courseId: String): Result<Boolean> = runCatching {
        userCourseEnrollmentsRepository.unenrollFromCourse(userId, courseId)
    }

    private fun resolveSortBy(sortBy: String): UserSortBy = when (sortBy.trim().lowercase()) {
        "email" -> UserSortBy.EMAIL
        "role" -> UserSortBy.ROLE
        "status" -> UserSortBy.STATUS
        "lastactivity", "lastactivityat", "last_activity_at" -> UserSortBy.LAST_ACTIVITY_AT
        else -> UserSortBy.CREATED_AT
    }

    private fun resolveSortOrder(sortOrder: String): SortOrder = when (sortOrder.trim().lowercase()) {
        "asc", "ascending" -> SortOrder.ASC
        else -> SortOrder.DESC
    }
}