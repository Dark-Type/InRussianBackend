package com.inRussian.services

import com.inRussian.models.users.User
import com.inRussian.models.users.UserRole
import com.inRussian.models.users.UserProfile
import com.inRussian.models.content.*
import com.inRussian.models.tasks.*
import com.inRussian.repositories.AdminRepository
import com.inRussian.repositories.ContentRepository
import java.math.BigDecimal
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


}

class ExpertServiceImpl(
    private val adminRepository: AdminRepository,
) : ExpertService {

    override suspend fun getAllStudents(
        page: Int,
        size: Int,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        sortBy: String,
        sortOrder: String
    ): Result<List<User>> = try {
        val students = adminRepository.getAllUsers(
            page = page,
            size = size,
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo,
            sortBy = sortBy,
            sortOrder = sortOrder
        )
        Result.success(students)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsCount(createdFrom: LocalDate?, createdTo: LocalDate?): Result<Long> = try {
        val count = adminRepository.getUsersCount(
            role = UserRole.STUDENT,
            createdFrom = createdFrom,
            createdTo = createdTo
        )
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsWithProfiles(page: Int, size: Int): Result<List<Pair<User, UserProfile?>>> = try {
        val studentsWithProfiles = adminRepository.getAllStudentsWithProfiles(page, size)
        Result.success(studentsWithProfiles)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsByCourse(courseId: String): Result<List<User>> = try {
        val students = adminRepository.getStudentsByCourseId(courseId)
        Result.success(students)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getStudentsCountByCourse(courseId: String): Result<Long> = try {
        val count = adminRepository.getStudentsCountByCourse(courseId)
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOverallStudentsCount(): Result<Long> = try {
        val count = adminRepository.getOverallStudentsCount()
        Result.success(count)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourseAverageTime(courseId: String): Result<Long?> = try {
        val time = adminRepository.getCourseAverageTime(courseId)
        Result.success(time)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getCourseAverageProgress(courseId: String): Result<Double?> = try {
        val progress = adminRepository.getCourseAverageProgress(courseId)
        Result.success(progress)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOverallAverageProgress(): Result<Double?> = try {
        val progress = adminRepository.getOverallAverageProgress()
        Result.success(progress)
    } catch (e: Exception) {
        Result.failure(e)
    }


    override suspend fun getOverallAverageTime(): Result<Long?> = try {
        val time = adminRepository.getOverallAverageTime()
        Result.success(time)
    } catch (e: Exception) {
        Result.failure(e)
    }
}