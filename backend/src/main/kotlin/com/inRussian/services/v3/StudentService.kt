package com.inRussian.services.v3

import com.inRussian.models.content.*
import com.inRussian.repositories.*
import java.util.UUID

interface StudentService {
    suspend fun getCoursesByUserLanguage(userId: String): Result<List<Course>>
    suspend fun enrollInCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>>
    suspend fun unenrollFromCourse(userId: String, courseId: String): Result<Boolean>
    suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?>
}

/**
 * Updated implementation:
 * - Validates UUIDs
 * - Checks user existence and course existence before mutating
 * - Returns descriptive errors in Result.failure
 */
class StudentServiceImpl(
    private val courses: CoursesRepository,
    private val enrollments: UserCourseEnrollmentsRepository,
    private val users: UsersRepository,
) : StudentService {

    private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess

    override suspend fun getCoursesByUserLanguage(userId: String): Result<List<Course>> = runCatching {
        require(userId.isUuid()) { "invalid_userId" }
        users.findById(userId) ?: throw NoSuchElementException("user_not_found")
        // Reuse existing logic if you have language info from user;
        // else, return all published courses (fallback).
        // If there is a repository method specialized by user language, call it here.
        courses.listAll().filter { it.isPublished }
    }

    override suspend fun enrollInCourse(userId: String, courseId: String): Result<Boolean> = runCatching {
        require(userId.isUuid()) { "invalid_userId" }
        require(courseId.isUuid()) { "invalid_courseId" }
        users.findById(userId) ?: throw NoSuchElementException("user_not_found")
        courses.findById(courseId) ?: throw NoSuchElementException("course_not_found")
        val ok = enrollments.enroll(userId, courseId)
        if (!ok) throw IllegalStateException("already_enrolled_or_failed")
        true
    }

    override suspend fun getUserEnrollments(userId: String): Result<List<UserCourseEnrollmentItem>> = runCatching {
        require(userId.isUuid()) { "invalid_userId" }
        users.findById(userId) ?: throw NoSuchElementException("user_not_found")
        enrollments.getUserEnrollments(userId)
    }

    override suspend fun unenrollFromCourse(userId: String, courseId: String): Result<Boolean> = runCatching {
        require(userId.isUuid()) { "invalid_userId" }
        require(courseId.isUuid()) { "invalid_courseId" }
        users.findById(userId) ?: throw NoSuchElementException("user_not_found")
        courses.findById(courseId) ?: throw NoSuchElementException("course_not_found")
        val removed = enrollments.unenrollFromCourse(userId, courseId)
        if (!removed) throw NoSuchElementException("enrollment_not_found")
        true
    }

    override suspend fun getCourseEnrollment(userId: String, courseId: String): Result<UserCourseEnrollmentItem?> =
        runCatching {
            require(userId.isUuid()) { "invalid_userId" }
            require(courseId.isUuid()) { "invalid_courseId" }
            users.findById(userId) ?: throw NoSuchElementException("user_not_found")
            courses.findById(courseId) ?: throw NoSuchElementException("course_not_found")
            enrollments.getCourseEnrollment(userId, courseId)
        }
}